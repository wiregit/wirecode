package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.httpclient.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.FileUtils;

import com.google.inject.Provider;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.tigertree.TigerTreeCache;

/**
 *  Allows the rest of LimeWire to treat this as a regular download. Handles downloading
 *  an item purchased off of the LimeWire Store (LWS) website
 */
public class StoreDownloader extends ManagedDownloader { 
    
    private static final Log LOG = LogFactory.getLog(StoreDownloader.class);
    
    public StoreDownloader(RemoteFileDesc rfd, 
            File saveDirectory, String fileName, boolean overwrite,
            SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            FileManager fileManager, IncompleteFileManager incompleteFileManager,
            DownloadCallback downloadCallback, NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory, RequeryManagerFactory requeryManagerFactory,
            QueryRequestFactory queryRequestFactory, OnDemandUnicaster onDemandUnicaster,
            DownloadWorkerFactory downloadWorkerFactory, AltLocManager altLocManager,
            ContentManager contentManager, SourceRankerFactory sourceRankerFactory,
            UrnCache urnCache, SavedFileManager savedFileManager,
            VerifyingFileFactory verifyingFileFactory, DiskController diskController,
            IPFilter ipFilter, ScheduledExecutorService backgroundExecutor,
            Provider<MessageRouter> messageRouter, Provider<TigerTreeCache> tigerTreeCache,
            ApplicationServices applicationServices) throws SaveLocationException {
        super(new RemoteFileDesc[]{rfd}, null,
                saveDirectory, fileName, overwrite, saveLocationManager, downloadManager, fileManager, incompleteFileManager,
                downloadCallback, networkManager, alternateLocationFactory, requeryManagerFactory,
                queryRequestFactory, onDemandUnicaster, downloadWorkerFactory, altLocManager,
                contentManager, sourceRankerFactory, urnCache, savedFileManager,
                verifyingFileFactory, diskController, ipFilter, backgroundExecutor, messageRouter,
                tigerTreeCache, applicationServices);
    } 
           
    
    ////////////////////////////// Requery Logic ///////////////////////////
    
    /** 
     * Overrides ManagedDownloader to return quickly
     * since we can't requery the store
     */
    @Override
    public QueryRequest newRequery()
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

        return new URLRemoteFileDesc(
                url.getHost(),  
                port,
                0l,             //index--doesn't matter since we won't push
                filename != null ? filename : MagnetOptions.extractFileName(uri),
                size <= 0 ? HTTPUtils.contentLength(url) : size,
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
