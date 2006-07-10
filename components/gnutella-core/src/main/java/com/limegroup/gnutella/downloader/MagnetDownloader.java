package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.StringUtils;

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
public class MagnetDownloader extends ManagedDownloader implements Serializable {

    private static final Log LOG = LogFactory.getLog(MagnetDownloader.class);

    /** Prevent versioning problems. */
    static final long serialVersionUID = 9092913030585214105L;

	private static final transient String MAGNET = "MAGNET"; 

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
	 *
     * @throws SaveLocationException if there was an error setting the downloads
     * final file location 
     */
    public MagnetDownloader(IncompleteFileManager ifm,
							MagnetOptions magnet,
							boolean overwrite,
                            File saveDir,
                            String fileName) throws SaveLocationException {
        //Initialize superclass with no locations.  We'll add the default
        //location when the download control thread calls tryAllDownloads.
        super(new RemoteFileDesc[0], ifm, null, saveDir, 
			  checkMagnetAndExtractFileName(magnet, fileName), overwrite);
		propertiesMap.put(MAGNET, magnet);
    }
    
    public void initialize(DownloadManager manager, FileManager fileManager, 
            DownloadCallback callback) {
		Assert.that(getMagnet() != null);
        downloadSHA1 = getMagnet().getSHA1Urn();
        super.initialize(manager, fileManager, callback);
    }

	private MagnetOptions getMagnet() {
		return (MagnetOptions)propertiesMap.get(MAGNET);
	}
    
    /**
     * overrides ManagedDownloader to ensure that we issue requests to the known
     * locations until we find out enough information to start the download 
     */
    protected int initializeDownload() {
        
		if (!hasRFD()) {
			MagnetOptions magnet = getMagnet();
			String[] defaultURLs = magnet.getDefaultURLs();
			if (defaultURLs.length == 0 )
				return Downloader.GAVE_UP;


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
				return GAVE_UP;
		}
        return super.initializeDownload();
    }
    
    /**
     * Overrides ManagedDownloader to ensure that the default location is tried.
     *
    protected int performDownload() {     

		for (int i = 0; _defaultURLs != null && i < _defaultURLs.length; i++) {
			//Send HEAD request to default location (if present)to get its size.
			//This can block, so it must be done here instead of in constructor.
			//See class overview and ManagedDownloader.tryAllDownloads.
            try {
                RemoteFileDesc defaultRFD = 
                    createRemoteFileDesc(_defaultURLs[i], _filename, _urn);
                
                //Add the faked up location before starting download. Note that 
                //we must force ManagedDownloader to accept this RFD in case 
                //it has no hash and a name that doesn't match the search 
                //keywords.
                super.addDownloadForced(defaultRFD,true);
                
            }catch(IOException badRFD) {
                if(LOG.isWarnEnabled())
                    LOG.warn("Ignoring magnet url: " + _defaultURLs[i]);
            }
		}

        //Start the downloads for real.
        return super.performDownload();
		}*/


    /** 
     * Creates a faked-up RemoteFileDesc to pass to ManagedDownloader.  If a URL
     * is provided, issues a HEAD request to get the file size.  If this fails,
     * returns null.  Package-access and static for easy testing.
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
                contentLength(url),
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

    /** Returns the length of the content at the given URL. 
     *  @exception IOException couldn't find the length for some reason */
    private static int contentLength(URL url) throws IOException {
        try {
            // Verify that the URL is valid.
            new URI(url.toExternalForm().toCharArray());
        } catch(URIException e) {
            //invalid URI, don't allow this URL.
            throw new IOException("invalid url: " + url);
        }

        HttpClient client = HttpClientManager.getNewClient();
        HttpMethod head = new HeadMethod(url.toExternalForm());
        head.addRequestHeader("User-Agent",
                              CommonUtils.getHttpServer());
        try {
            client.executeMethod(head);
            //Extract Content-length, but only if the response was 200 OK.
            //Generally speaking any 2xx response is ok, but in this situation
            //we expect only 200.
            if (head.getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Got " + head.getStatusCode() +
                                      " instead of 200");
            
            int length = head.getResponseContentLength();
            if (length<0)
                throw new IOException("No content length");
            return length;
        } finally {
            if(head != null)
                head.releaseConnection();
        }
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
            String q = StringUtils.createQueryString(textQuery);
            return QueryRequest.createQuery(q);
        }
        else {
            String q = StringUtils.createQueryString(getSaveFile().getName());
            return QueryRequest.createQuery(q);
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
	 * Creates a magnet downloader object when converting from the old 
	 * downloader version.
	 * 
	 * @throws IOException when the created magnet is not downloadable
	 */
	private void readObject(ObjectInputStream stream)
	throws IOException, ClassNotFoundException {
        MagnetOptions magnet = getMagnet();
		if (magnet == null) {
			ObjectInputStream.GetField fields = stream.readFields();
			String textQuery = (String) fields.get("_textQuery", null);
			URN urn = (URN) fields.get("_urn", null);
			String fileName = (String) fields.get("_filename", null);
			String[] defaultURLs = (String[])fields.get("_defaultURLs", null);
			magnet = MagnetOptions.createMagnet(textQuery, fileName, urn, defaultURLs);
			if (!magnet.isDownloadable()) {
				throw new IOException("Old undownloadable magnet");
			}
			propertiesMap.put(MAGNET, magnet);
		}
        
        if (propertiesMap.get(DEFAULT_FILENAME) == null) 
            propertiesMap.put(DEFAULT_FILENAME, magnet.getFileNameForSaving());
        
    }

    /**
	 * Only allow requeries when <code>downloadSHA1</code> is not null.
     */
	protected boolean shouldSendRequeryImmediately(int numRequeries) {
		return downloadSHA1 != null ? super.shouldSendRequeryImmediately(numRequeries) 
				: false;
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
}
