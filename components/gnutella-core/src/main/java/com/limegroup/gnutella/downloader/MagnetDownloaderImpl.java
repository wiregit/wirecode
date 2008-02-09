package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.HttpException;
import org.limewire.io.InvalidDataException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.MagnetDownloadMemento;
import com.limegroup.gnutella.downloader.serial.MagnetDownloadMementoImpl;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.util.QueryUtils;

/**
 * A ManagedDownloader for MAGNET URIs.  Unlike a ManagedDownloader, a
 * MagnetDownloader need not have an initial RemoteFileDesc.  Instead it can be
 * started with various combinations of the following:
 * <ul>
 * <li>initial URL (exact source)
 * <li>hash/URN (exact topic)
 * <li>file name (display name)
 * <li>search keywords (keyword topic)
 * </ul>
 * Names in parentheses are those given by the MAGNET specification at
 * http://magnet-uri.sourceforge.net/magnet-draft-overview.txt
 * <p>
 * Implementation note: this uses ManagedDownloader to try the initial download
 * location.  Unfortunately ManagedDownloader requires RemoteFileDesc's.  We can
 * fake up most of the RFD fields, but size presents problems.
 * ManagedDownloader depends on size for swarming purposes.  It is possible to
 * redesign the swarming algorithm to work around the lack of size, but this is
 * complex, especially with regard to HTTP/1.1 swarming.  For this reason, we
 * simply make a HEAD request to get the content length before starting the
 * download.  
 */
class MagnetDownloaderImpl extends ManagedDownloaderImpl implements MagnetDownloader {
        
	private MagnetOptions magnet;

    /**
     * Creates a new MAGNET downloader.  Immediately tries to download from
     * <tt>defaultURLs</tt>, if specified. If that fails, or if defaultURLs does
     * not provide alternate locations, issues a requery with <tt>textQuery</tt>
     * and </tt>urn</tt>, as provided.  (Note that at least one must be
     * non-null.)  If <tt>filename</tt> is specified, it will be used as the
     * name of the complete file; otherwise it will be taken from any search
     * results or guessed from <tt>defaultURLs</tt>.
     * @param saveLocationManager 
     * @param pushListProvider TODO
     * @param magnet contains all the information for the download, must be
     * {@link MagnetOptions#isDownloadable() downloadable}.
     * @param overwrite whether file at download location should be overwritten
     * @param saveDir can be null, then the default save directory is used
     * @param fileName the final file name, can be <code>null</code>
     *
     * @throws SaveLocationException if there was an error setting the downloads
     * final file location 
     */
	@Inject
    MagnetDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            FileManager fileManager, IncompleteFileManager incompleteFileManager,
            DownloadCallback downloadCallback, NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory,
            RequeryManagerFactory requeryManagerFactory, QueryRequestFactory queryRequestFactory,
            OnDemandUnicaster onDemandUnicaster, DownloadWorkerFactory downloadWorkerFactory,
            AltLocManager altLocManager, ContentManager contentManager,
            SourceRankerFactory sourceRankerFactory, UrnCache urnCache,
            SavedFileManager savedFileManager, VerifyingFileFactory verifyingFileFactory,
            DiskController diskController, 
            IPFilter ipFilter, @Named("backgroundExecutor")
            ScheduledExecutorService backgroundExecutor, Provider<MessageRouter> messageRouter,
            Provider<HashTreeCache> tigerTreeCache, ApplicationServices applicationServices,
            RemoteFileDescFactory remoteFileDescFactory, Provider<PushList> pushListProvider) {
        super(saveLocationManager, downloadManager, fileManager, incompleteFileManager,
                downloadCallback, networkManager, alternateLocationFactory, requeryManagerFactory,
                queryRequestFactory, onDemandUnicaster, downloadWorkerFactory, altLocManager,
                contentManager, sourceRankerFactory, urnCache, savedFileManager,
                verifyingFileFactory, diskController, ipFilter, backgroundExecutor, messageRouter,
                tigerTreeCache, applicationServices, remoteFileDescFactory, pushListProvider);
    }
    
    public void initialize() {
        assert (getMagnet() != null);
        URN sha1 = getMagnet().getSHA1Urn();
        if(sha1 != null)
            setSha1Urn(sha1);
        super.initialize();
    }

	protected synchronized MagnetOptions getMagnet() {
	    return magnet;
	}
	
	public synchronized void setMagnet(MagnetOptions magnet) {
        if(getMagnet() != null)
            throw new IllegalStateException("magnet already set!");
        this.magnet = magnet;
	}
    
    /**
     * overrides ManagedDownloader to ensure that we issue requests to the known
     * locations until we find out enough information to start the download 
     */
    protected DownloadStatus initializeDownload() {
        
		if (!hasRFD()) {
			MagnetOptions magnet = getMagnet();
			String[] defaultURLs = magnet.getDefaultURLs();
			if (defaultURLs.length == 0 )
				return DownloadStatus.GAVE_UP;


			RemoteFileDesc firstDesc = null;
			
			for (int i = 0; i < defaultURLs.length && firstDesc == null; i++) {
				try {
					firstDesc = createRemoteFileDesc(defaultURLs[i],
													 getSaveFile().getName(), magnet.getSHA1Urn());
							
					initPropertiesMap(firstDesc);
					addDownloadForced(firstDesc, true);
				} catch (IOException badRFD) {} 
                  catch (HttpException e) {} 
                  catch (URISyntaxException e) {} 
                  catch (InterruptedException e) {}
            }
        
			// if all locations included in the magnet URI fail we can't do much
			if (firstDesc == null)
				return DownloadStatus.GAVE_UP;
		}
        return super.initializeDownload();
    }
    
    /** 
     * Creates a faked-up RemoteFileDesc to pass to ManagedDownloader.  If a URL
     * is provided, issues a HEAD request to get the file size.  If this fails,
     * returns null.  Package-access and static for easy testing.
     * 
     * NOTE: this calls HTTPUtils.contentLength which opens a URL and calls Head on the
     * link to determine the file length. This is a blocking call!
     */
    @SuppressWarnings("deprecation")
    private RemoteFileDesc createRemoteFileDesc(String defaultURL,
        String filename, URN urn)
            throws IOException, HttpException, InterruptedException, URISyntaxException {
        return remoteFileDescFactory.createUrlRemoteFileDesc(new URL(defaultURL), filename, urn, -1L);
    } 

    ////////////////////////////// Requery Logic ///////////////////////////

    /** 
     * Overrides ManagedDownloader to use the query words 
     * specified by the MAGNET URI.
     */
    public QueryRequest newRequery()
        throws CantResumeException {
        MagnetOptions magnet = getMagnet();
		String textQuery = magnet.getQueryString();
        if (textQuery != null) {
            String q = QueryUtils.createQueryString(textQuery);
            return queryRequestFactory.createQuery(q);
        }
        else {
            String q = QueryUtils.createQueryString(getSaveFile().getName());
            return queryRequestFactory.createQuery(q);
        }
    }

    /** 
     * Overrides ManagedDownloader to allow any files with the right
     * hash even if this doesn't currently have any download
     * locations.  
     * <p>
     * We only allow for additions if the download has a sha1.  
     */
    protected boolean allowAddition(RemoteFileDesc other) {        
        // Allow if we have a hash and other matches it.
		URN otherSHA1 = other.getSHA1Urn();
		if (getSha1Urn() != null && otherSHA1 != null) {
			return getSha1Urn().equals(otherSHA1);
        }
        return false;
    }

    /**
	 * Overridden for internal purposes, returns result from super method
	 * call.
     */
	protected synchronized boolean addDownloadForced(RemoteFileDesc rfd,
													 boolean cache) {
		if (!hasRFD())
			initPropertiesMap(rfd);
		return super.addDownloadForced(rfd, cache);
	}

    /**
	 * Only allow requeries when <code>downloadSHA1</code> is not null.
     */
	protected boolean canSendRequeryNow() {
		return getSha1Urn() != null ? super.canSendRequeryNow() : false;
	}

	/**
	 * Overridden to make sure it calls the super method only if 
	 * the filesize is known.
	 */
	protected void initializeIncompleteFile() throws IOException {
		if (getContentLength() != -1) {
			super.initializeIncompleteFile();
		}
	}
    
    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.MAGNET;
    }
    
    @Override
    public synchronized void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        super.initFromMemento(memento);
        MagnetDownloadMemento mmem = (MagnetDownloadMemento)memento;
        setMagnet(mmem.getMagnet());
        
    }
    
    @Override
    protected void fillInMemento(DownloadMemento memento) {
        super.fillInMemento(memento);
        MagnetDownloadMemento mmem = (MagnetDownloadMemento)memento;
        mmem.setMagnet(getMagnet());
    }
    
    @Override
    protected DownloadMemento createMemento() {
        return new MagnetDownloadMementoImpl();
    }
}
