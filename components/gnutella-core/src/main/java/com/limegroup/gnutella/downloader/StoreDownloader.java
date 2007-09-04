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
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.store.StoreDescriptor;

/**
 *  Allows the rest of LimeWire to treat this as a regular download. Handles downloading
 *  an item purchased off of the LimeWire Store website
 */
public class StoreDownloader extends ManagedDownloader implements Serializable { 
    
    private static final Log LOG = LogFactory.getLog(StoreDownloader.class);
    
    private static final transient String STORE = "STORE"; 
    
    public StoreDownloader(StoreDescriptor store, IncompleteFileManager ifc, 
            File saveDirectory, String fileName, boolean overwrite,
            SaveLocationManager manager) throws SaveLocationException {
        super(new RemoteFileDesc[0], ifc, null,
                saveDirectory, fileName, overwrite, manager);
        synchronized(this) {
            propertiesMap.put(STORE, store);
        }
    } 
    
    public void initialize(DownloadReferences downloadReferences) {
        assert(getStore() != null);
        downloadSHA1 = getStore().getSHA1Urn();
        super.initialize(downloadReferences);
    }
    
    private synchronized StoreDescriptor getStore() {
        return (StoreDescriptor)propertiesMap.get(STORE);
    }
    
    /**
     * overrides ManagedDownloader to ensure that we issue requests to the known
     * locations until we find out enough information to start the download 
     */
    @Override
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
                size <= 0 ? contentLength(url) : size,
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
     * Overrides ManagedDownloader to return quickly
     * since we can't require the store
     */
    @Override
    protected QueryRequest newRequery(int numRequeries)
        throws CantResumeException {
            return null;
    }
    
    /** 
     * Overrides ManagedDownloader to never allow new sources to be
     * added
     */
    @Override
    protected boolean allowAddition(RemoteFileDesc other) {        
        return false;
    }
    
    /**
     * Never send requires since there is only one location to download from,
     * the LWS.
     */
    @Override
    protected boolean canSendRequeryNow() {
        return false;
    }
    
    
    /**
     * Overridden to make sure it calls the super method only if 
     * the filesize is known.
     */
    @Override
    protected void initializeIncompleteFile() throws IOException {
        if (getContentLength() != -1) {
            super.initializeIncompleteFile();
        }
    }
       
    /**
     * Can never chat with LWS, return immediately
     */
    @Override
    public synchronized Endpoint getChatEnabledHost() {
        return null;
    }

    /**
     * Can never chat with LWS, always return false
     */
    @Override
    public synchronized boolean hasChatEnabledHost() {
        return false;
    }

    /**
     * Can never browse the LWS, return immediately
     */
    @Override
    public synchronized RemoteFileDesc getBrowseEnabledHost() {
        return null;
    }

    /**
     * Can never browse the LWS, return immediately
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
    

    /**
     * Override the save file to keep the file from being added to the
     * shared folder directory
     */
    @Override
    protected DownloadStatus saveFile(URN fileHash){
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
        
        // First attempt to get new save location
        final File realOutputDir = SharingSettings.getSaveLWSDirectory(incompleteFile);

        // make sure it is writable
        if (!FileUtils.setWriteable(realOutputDir)) {
            reportDiskProblem("could not set file writeable " + 
                    getSaveFile().getParentFile());
            return DownloadStatus.DISK_PROBLEM;
        }
        // move file to new folder
        saveFile = new File(realOutputDir, saveFile.getName());
        boolean success = FileUtils.forceRename(incompleteFile,saveFile);

        //TODO: should we remove it after a success? ths has been downloaded
        //      succefully but just left in the incomplete folder
        incompleteFileManager.removeEntry(incompleteFile);
        
        // If that didn't work, we're out of luck.
        if (!success) {
            reportDiskProblem("forceRename failed "+incompleteFile+
                    " -> "+ saveFile);
            return DownloadStatus.DISK_PROBLEM;
        }
            
        //try removing the file from being shared in case it was added
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
            urnCache.addUrns(file, urns);
            // Notify the SavedFileManager that there is a new saved
            // file.
            savedFileManager.addSavedFile(file, urns);
        }
        return DownloadStatus.COMPLETE;
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


}
