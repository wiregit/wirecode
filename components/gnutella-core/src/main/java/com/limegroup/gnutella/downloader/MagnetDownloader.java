package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.apache.commons.httpclient.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.messages.QueryRequest;
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
public class MagnetDownloader extends ManagedDownloader {

    private static final Log LOG = LogFactory.getLog(MagnetDownloader.class);
    
	private static final String MAGNET = "MAGNET"; 

    /**
     * Creates a new MAGNET downloader.  Immediately tries to download from
     * <tt>defaultURLs</tt>, if specified. If that fails, or if defaultURLs does
     * not provide alternate locations, issues a requery with <tt>textQuery</tt>
     * and </tt>urn</tt>, as provided.  (Note that at least one must be
     * non-null.)  If <tt>filename</tt> is specified, it will be used as the
     * name of the complete file; otherwise it will be taken from any search
     * results or guessed from <tt>defaultURLs</tt>.
     *
     * @param magnet contains all the information for the download, must be
     * {@link MagnetOptions#isDownloadable() downloadable}.
     * @param overwrite whether file at download location should be overwritten
     * @param saveDir can be null, then the default save directory is used
	 * @param fileName the final file name, can be <code>null</code>
     * @param saveLocationManager 
	 *
     * @throws SaveLocationException if there was an error setting the downloads
     * final file location 
     */
    MagnetDownloader(IncompleteFileManager ifm,
							MagnetOptions magnet,
							boolean overwrite,
                            File saveDir,
                            String fileName, SaveLocationManager saveLocationManager) throws SaveLocationException {
        //Initialize superclass with no locations.  We'll add the default
        //location when the download control thread calls tryAllDownloads.
        super(new RemoteFileDesc[0], ifm, null, saveDir, 
			  checkMagnetAndExtractFileName(magnet, fileName), overwrite, saveLocationManager);
        synchronized(this) {
            propertiesMap.put(MAGNET, magnet);
        }
    }
    
    public void initialize(DownloadReferences downloadReferences) {
        assert (getMagnet() != null);
        downloadSHA1 = getMagnet().getSHA1Urn();
        super.initialize(downloadReferences);
    }

	private synchronized MagnetOptions getMagnet() {
		return (MagnetOptions)propertiesMap.get(MAGNET);
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
    private static RemoteFileDesc createRemoteFileDesc(String defaultURL,
        String filename, URN urn) throws IOException{
        if (defaultURL==null) {
            LOG.debug("createRemoteFileDesc called with null URL");        
            return null;
        }

        URL url = null;
        // Use the URL class to do a little parsing for us.
        url = new URL(defaultURL);
        int port = url.getPort();
        if (port<0)
            port=80;      //assume default for HTTP (not 6346)
        
        Set<URN> urns= new UrnSet();
        if (urn!=null)
            urns.add(urn);
        
        URI uri = new URI(url);
        
        return new URLRemoteFileDesc(
                url.getHost(),  
                port,
                0l,             //index--doesn't matter since we won't push
                filename != null ? filename : MagnetOptions.extractFileName(uri),
                HTTPUtils.contentLength(url),
                new byte[16],   //GUID--doesn't matter since we won't push
                SpeedConstants.T3_SPEED_INT,
                false,          //no chat support
                3,              //four [sic] star quality
                false,          //no browse host
                null,           //no metadata
                urns,
                false,          //not a reply to a multicast query
                false,"",       //not firewalled, no vendor,
                url,            //url for GET request
                null,           //no push proxies
                0);         //assume no firewall transfer
    } 

    ////////////////////////////// Requery Logic ///////////////////////////

    /** 
     * Overrides ManagedDownloader to use the query words 
     * specified by the MAGNET URI.
     */
    protected QueryRequest newRequery(int numRequeries)
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
		if (downloadSHA1 != null && otherSHA1 != null) {
			return downloadSHA1.equals(otherSHA1);
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
		return downloadSHA1 != null ? super.canSendRequeryNow() : false;
	}

	/**
	 * Checks if the magnet is downloadable and extracts a fileName if
	 * <code>fileName</code> is null.
	 *
	 * @throws IllegalArgumentException if the magnet is not downloadable
	 */
	private static String checkMagnetAndExtractFileName(MagnetOptions magnet, 
														String fileName) {
		if (!magnet.isDownloadable()) {
			throw new IllegalArgumentException("magnet not downloadable");
		}
		if (fileName != null) {
			return fileName;
		}
		return magnet.getFileNameForSaving();
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
}
