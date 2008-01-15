package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.limewire.util.FileUtils;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.SharingSettings;

/**
 *  Allows the rest of LimeWire to treat this as a regular download. Handles downloading
 *  an item purchased off of the LimeWire Store (LWS) website
 */
public class StoreDownloader extends ManagedDownloader implements Serializable { 
    
    private static final long serialVersionUID = 1672575739103885243L;
    
    private static final Log LOG = LogFactory.getLog(StoreDownloader.class);
        
    public StoreDownloader(RemoteFileDesc rfd, IncompleteFileManager ifc, 
            File saveDirectory, String fileName, boolean overwrite,
            SaveLocationManager manager) throws SaveLocationException {
        super(new RemoteFileDesc[]{rfd}, ifc, null,
                saveDirectory, fileName, overwrite, manager);
    } 
           
    
    ////////////////////////////// Requery Logic ///////////////////////////
    
    /** 
     * Overrides ManagedDownloader to return quickly
     * since we can't requery the store
     */
    @Override
    public QueryRequest newRequery(int numRequeries)
        throws CantResumeException {
            return null;
    }
    
    /** 
     * Overrides ManagedDownloader to never allow new sources to be
     * added
     */
    @Override
    public boolean allowAddition(RemoteFileDesc other) {        
        return false;
    }
    
    /**
     * Never send requires since there is only one location to download from,
     * the LWS.
     */
    @Override
    public boolean canSendRequeryNow() {
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
    public Endpoint getChatEnabledHost() {
        return null;
    }

    /**
     * Can never chat with LWS, always return false
     */
    @Override
    public boolean hasChatEnabledHost() {
        return false;
    }

    /**
     * Can never browse the LWS, return immediately
     */
    @Override
    public RemoteFileDesc getBrowseEnabledHost() {
        return null;
    }

    /**
     * Can never browse the LWS, return immediately
     */
    @Override
    public boolean hasBrowseEnabledHost() {
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
     * Use the file ID3 info to perform a lookup with the template to determine the
     * folder substructure for saving the file
     */
    @Override
    protected File getSuggestedSaveLocation(File saveFile) throws IOException{
        // First attempt to get new save location
        final File realOutputDir = SharingSettings.getSaveLWSDirectory(incompleteFile);

        // make sure it is writable
        if (!FileUtils.setWriteable(realOutputDir)) {
            reportDiskProblem("could not set file writeable " + 
                    getSaveFile().getParentFile());
            throw new IOException("Disk Error");
        } 
        // move file to new folder
        return new File(realOutputDir, saveFile.getName());
    }
    
   
    /**
     * Store files aren't shared so don't bother saving the tree
     * hash of them
     */
    @Override
    protected URN saveTreeHash(URN fileHash) {
        return null;
    }
    
    /**
     * Not sharing so do nothing
     */
    @Override
    protected void shareSavedFile() {
        // dont do anything
    }
    
    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.STORE;
    }
    
    /**
     * Overrides the entry to a new download into the file manager to ensure that the 
     * file is not shared
     */
    @Override
    protected void addAndRegisterIncompleteFile(){
        incompleteFileManager.addEntry(incompleteFile, commonOutFile, true);
    }
    
    /** 
     * Creates a faked-up RemoteFileDesc to pass to ManagedDownloader. File size
     * should always be passed in, if not this method will do a lookup using the URL
     * to retrieve HEAD which will result in this method blocking
     */
    @SuppressWarnings("deprecation")
    public static RemoteFileDesc createRemoteFileDesc(URL url,
        String filename, URN urn, long size) throws IOException, HttpException, InterruptedException {
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
        
        URI uri = null;
        try {
            uri = new URI(url.toString());
        } catch (URISyntaxException e) {
            ErrorService.error(e);
            IOException ioe = new IOException("malormed URL: " + url);
            ioe.initCause(e);
            throw ioe;
        }  

        return new URLRemoteFileDesc(
                url.getHost(),  
                port,
                0l,             //index--doesn't matter since we won't push
                filename != null ? filename : MagnetOptions.extractFileName(uri),
                size <= 0 ? HTTPUtils.contentLength(uri) : size,
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
    
    



}
