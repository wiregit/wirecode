package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
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

import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.store.StoreDescriptor;

/**
 *  Allows the rest of LimeWire to treat this as a regular download
 */
public class StoreDownloader extends ManagedDownloader implements Serializable { 
    
    private static final Log LOG = LogFactory.getLog(StoreDownloader.class);
    
    private static final transient String STORE = "STORE"; 
    
    public StoreDownloader(StoreDescriptor store, IncompleteFileManager ifc, 
            File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException {
        super(new RemoteFileDesc[0], ifc, null,
                saveDirectory, fileName, overwrite);
        synchronized(this) {
            propertiesMap.put(STORE, store);
        }
    } 
    
    public void initialize(DownloadManager manager, FileManager fileManager, 
            DownloadCallback callback) {
        assert(getStore() != null);
        downloadSHA1 = getStore().getSHA1Urn();
        super.initialize(manager, fileManager, callback);
    }
    
    private synchronized StoreDescriptor getStore() {
        return (StoreDescriptor)propertiesMap.get(STORE);
    }
    
    /**
     * overrides ManagedDownloader to ensure that we issue requests to the known
     * locations until we find out enough information to start the download 
     */
    protected DownloadStatus initializeDownload() {
        
        if (!hasRFD()) {
            StoreDescriptor store = getStore();
            URL url = store.getURL();
            if (url == null )
                return DownloadStatus.GAVE_UP;

            RemoteFileDesc firstDesc = null;
            
            try {
                firstDesc = createRemoteFileDesc(url,
                                           getSaveFile().getName(), store.getSHA1Urn(), 
                                           store.getSize());
                initPropertiesMap(firstDesc);
                addDownloadForced(firstDesc, true);
            } catch (IOException badRFD) {}

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
     */
    @SuppressWarnings("deprecation")
    private static RemoteFileDesc createRemoteFileDesc(URL url,
        String filename, URN urn, long size) throws IOException{
        if (url==null) {
            LOG.debug("createRemoteFileDesc called with null URL");        
            return null;
        }

        // Use the URL class to do a little parsing for us.

        int port = url.getPort();
        if (port<0)
            port=80;      //assume default for HTTP (not 6346)
        
        Set<URN> urns= new UrnSet();
        if (urn!=null)
            urns.add(urn);
        
        URI uri = new URI(url);

        
        //TODO: filename needs to get cleaned up here and a check on file size
        return new URLRemoteFileDesc(
                url.getHost(),  
                port,
                0l,             //index--doesn't matter since we won't push
                filename != null ? filename : MagnetOptions.extractFileName(uri),
                size,//contentLength(url),
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
    private static long contentLength(URL url) throws IOException {
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
                              LimeWireUtils.getHttpServer());
        try {
            client.executeMethod(head);
            //Extract Content-length, but only if the response was 200 OK.
            //Generally speaking any 2xx response is ok, but in this situation
            //we expect only 200.
            if (head.getStatusCode() != HttpStatus.SC_OK)
                throw new IOException("Got " + head.getStatusCode() +
                                      " instead of 200");
            
            long length = head.getResponseContentLength();
            if (length<0)
                throw new IOException("No content length");
            return length;
        } finally {
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
            return null;
    }
    
    /** 
     * Overrides ManagedDownloader to never allow new sources to be
     * added
     */
    protected boolean allowAddition(RemoteFileDesc other) {        
        return false;
    }
    
    /**
     * Never send requires since there is only one location to download from.
     */
    protected boolean canSendRequeryNow() {
        return false;
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
    
//    private synchronized void addQueuedWorker(DownloadWorker queued, int position) {
//        if (LOG.isDebugEnabled())
//            LOG.debug("adding queued worker " + queued +" at position "+position+
//                    " current queued workers:\n"+_queuedWorkers);
//        
//        if(!_workers.contains(queued))
//            throw new IllegalStateException("attempting to queue invalid worker: " + queued);
//        
//        if ( position < queuePosition ) {
//            queuePosition = position;
//            queuedVendor = queued.getDownloader().getVendor();
//        }
//        Map<DownloadWorker, Integer> m = new HashMap<DownloadWorker, Integer>(getQueuedWorkers());
//        m.put(queued, new Integer(position));
//        _queuedWorkers = Collections.unmodifiableMap(m);
//    }
    
    /**
     * Can never chat with LWS
     */
    @Override
    public synchronized Endpoint getChatEnabledHost() {
        return null;
    }

    /**
     * Can never chat with LWS
     */
    @Override
    public synchronized boolean hasChatEnabledHost() {
        return false;
    }

    /**
     * Can never browse the LWS
     */
    @Override
    public synchronized RemoteFileDesc getBrowseEnabledHost() {
        return null;
    }

    /**
     * Can never browse the LWS
     */
    @Override
    public synchronized boolean hasBrowseEnabledHost() {
        return false;
    }
    
    /**
     * Can only connect to LWS on one socket
     */
    @Override
    public int getNumberOfAlternateLocations() {
        return 0;
    }

    /**
     * Can only connect to LWS
     */
    @Override
    public int getNumberOfInvalidAlternateLocations() {
        return 0;
    }
    


    /*
        private DownloadStatus saveFile(URN fileHash){
        // let the user know we're saving the file...
        setState( DownloadStatus.SAVING );
        
        //4. Move to library.
        // Make sure we can write into the complete file's directory.
        if (!FileUtils.setWriteable(getSaveFile().getParentFile())) {
            reportDiskProblem("could not set file writeable " + 
                    getSaveFile().getParentFile());
            return DownloadStatus.DISK_PROBLEM;
        }
        File saveFile = getSaveFile();
        //Delete target.  If target doesn't exist, this will fail silently.
        saveFile.delete();

        //Try moving file.  If we couldn't move the file, i.e., because
        //someone is previewing it or it's on a different volume, try copy
        //instead.  If that failed, notify user.  
        //   If move is successful, we should remove the corresponding blocks
        //from the IncompleteFileManager, though this is not strictly necessary
        //because IFM.purge() is called frequently in DownloadManager.
        
        // First attempt to rename it.
        boolean success = FileUtils.forceRename(incompleteFile,saveFile);

        incompleteFileManager.removeEntry(incompleteFile);
        
        // If that didn't work, we're out of luck.
        if (!success) {
            reportDiskProblem("forceRename failed "+incompleteFile+
                    " -> "+ saveFile);
            return DownloadStatus.DISK_PROBLEM;
        }
            
        //Add file to library.
        // first check if it conflicts with the saved dir....
        if (saveFile.exists())
            fileManager.removeFileIfShared(saveFile);

        //Add the URN of this file to the cache so that it won't
        //be hashed again when added to the library -- reduces
        //the time of the 'Saving File' state.
        if(fileHash != null) {
            Set<URN> urns = new UrnSet(fileHash);
            File file = saveFile;
            try {
                file = FileUtils.getCanonicalFile(saveFile);
            } catch(IOException ignored) {}
            // Always cache the URN, so results can lookup to see
            // if the file exists.
            UrnCache.instance().addUrns(file, urns);
            // Notify the SavedFileManager that there is a new saved
            // file.
            SavedFileManager.instance().addSavedFile(file, urns);
            
            // save the trees!
            if (downloadSHA1 != null && downloadSHA1.equals(fileHash) && commonOutFile.getHashTree() != null) {
                TigerTreeCache.instance(); 
                TigerTreeCache.addHashTree(downloadSHA1,commonOutFile.getHashTree());
            }
        }

        
        if (SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue())
            fileManager.addFileAlways(getSaveFile(), getXMLDocuments());
        else
            fileManager.addFileIfShared(getSaveFile(), getXMLDocuments());

        return DownloadStatus.COMPLETE;
    }
    */

}
