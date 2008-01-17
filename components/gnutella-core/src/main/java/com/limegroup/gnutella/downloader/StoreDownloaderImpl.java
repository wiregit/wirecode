package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
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
class StoreDownloaderImpl extends ManagedDownloaderImpl implements StoreDownloader {
    
    @Inject
    public StoreDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            FileManager fileManager, IncompleteFileManager incompleteFileManager,
            DownloadCallback downloadCallback, NetworkManager networkManager,
            AlternateLocationFactory alternateLocationFactory, RequeryManagerFactory requeryManagerFactory,
            QueryRequestFactory queryRequestFactory, OnDemandUnicaster onDemandUnicaster,
            DownloadWorkerFactory downloadWorkerFactory, AltLocManager altLocManager,
            ContentManager contentManager, SourceRankerFactory sourceRankerFactory,
            UrnCache urnCache, SavedFileManager savedFileManager,
            VerifyingFileFactory verifyingFileFactory, DiskController diskController,
            @Named("ipFilter") IPFilter ipFilter, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<MessageRouter> messageRouter, Provider<TigerTreeCache> tigerTreeCache,
            ApplicationServices applicationServices, RemoteFileDescFactory remoteFileDescFactory) {
        super(saveLocationManager, downloadManager, fileManager, incompleteFileManager,
                downloadCallback, networkManager, alternateLocationFactory, requeryManagerFactory,
                queryRequestFactory, onDemandUnicaster, downloadWorkerFactory, altLocManager,
                contentManager, sourceRankerFactory, urnCache, savedFileManager,
                verifyingFileFactory, diskController, ipFilter, backgroundExecutor, messageRouter,
                tigerTreeCache, applicationServices, remoteFileDescFactory);
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
        final File realOutputDir = SharingSettings.getSaveLWSDirectory(getIncompleteFile());

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
        incompleteFileManager.addEntry(getIncompleteFile(), commonOutFile, true);
    } 
    
    



}
