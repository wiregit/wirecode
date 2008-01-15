package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.version.DownloadInformation;

/**
 * A downloader that works in the background, using the network to continue itself.
 */
class InNetworkDownloaderImpl extends ManagedDownloaderImpl implements InNetworkDownloader {
    
    private static String TT_ROOT = "innetwork.ttRoot";
    private static String START_TIME = "innetwork.startTime";
    private static String DOWNLOAD_ATTEMPTS = "innetwork.downloadAttempts";
    
    /** 
     * Constructs a new downloader that's gonna work off the network.
     */
    @Inject
    InNetworkDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
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
            ApplicationServices applicationServices) throws SaveLocationException {
        super(saveLocationManager, downloadManager, fileManager, incompleteFileManager,
                downloadCallback, networkManager, alternateLocationFactory, requeryManagerFactory,
                queryRequestFactory, onDemandUnicaster, downloadWorkerFactory, altLocManager,
                contentManager, sourceRankerFactory, urnCache, savedFileManager,
                verifyingFileFactory, diskController, ipFilter, backgroundExecutor, messageRouter,
                tigerTreeCache, applicationServices);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.InNetworkDownloader#initDownloadInformation(com.limegroup.gnutella.version.DownloadInformation, long)
     */
    public void initDownloadInformation(DownloadInformation downloadInformation, long startTime) {
        // note: even though we support bigger files, this is a good sanity check
        if (downloadInformation.getSize() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("size too big for now.");
        propertiesMap.put(FILE_SIZE, downloadInformation.getSize());
        propertiesMap.put(SHA1_URN, downloadInformation.getUpdateURN());
        propertiesMap.put(TT_ROOT, downloadInformation.getTTRoot());
        propertiesMap.put(START_TIME, startTime);
        propertiesMap.put(DOWNLOAD_ATTEMPTS, 0);
    }    
    
    /**
     * Overriden to use a different incomplete directory.
     */
    @Override
    protected File getIncompleteFile(String name, URN urn,
                                     long length) throws IOException {
        return incompleteFileManager.getFile(name, urn, length, new File(SharingUtils.PREFERENCE_SHARE, "Incomplete"));
    }
    
    /**
     * Gets a new SourceRanker, using only LegacyRanker (not PingRanker).
     */
    @Override
    protected SourceRanker getSourceRanker(SourceRanker oldRanker) {
        if(oldRanker != null)
            return oldRanker;
        else
            return new LegacyRanker();
    }
        
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.InNetworkDownloader#startDownload()
     */
    @Override
    public synchronized void startDownload() {
        Integer i = (Integer)propertiesMap.get(DOWNLOAD_ATTEMPTS);
        if(i == null)
            i = 1;
        else
            i = i+1;
        propertiesMap.put(DOWNLOAD_ATTEMPTS, i);
        super.startDownload();
    }
    
    @Override
    protected boolean shouldValidate() {
        return false;
    }
    
    /**
     * Ensures that the VerifyingFile knows what TTRoot we're expecting.
     */
    @Override
    protected void initializeVerifyingFile() throws IOException {
        super.initializeVerifyingFile();
        if(commonOutFile != null) {
            commonOutFile.setExpectedHashTreeRoot((String)propertiesMap.get(TT_ROOT));
        }
    }
    
    /**
     * Sends a targeted query for this.
     */
    @Override
    public synchronized QueryRequest newRequery() throws CantResumeException {
        QueryRequest qr = super.newRequery();
        qr.setTTL((byte) 2);
        return qr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.downloader.InNetworkDownloader#getNumAttempts()
     */
    public synchronized int getNumAttempts() {
        return (Integer)propertiesMap.get(DOWNLOAD_ATTEMPTS);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.InNetworkDownloader#getStartTime()
     */
    public long getStartTime() {
        return (Long)propertiesMap.get(START_TIME);
    }
    
    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.INNETWORK;
    }
}
