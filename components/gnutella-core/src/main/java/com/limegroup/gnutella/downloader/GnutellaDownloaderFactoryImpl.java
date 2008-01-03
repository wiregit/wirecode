package com.limegroup.gnutella.downloader;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.version.DownloadInformation;

@Singleton
public class GnutellaDownloaderFactoryImpl implements GnutellaDownloaderFactory {

    private final Provider<SaveLocationManager> saveLocationManager;
    private Provider<FileManager> fileManager;
    private Provider<DownloadManager> downloadManager;
    private Provider<IncompleteFileManager> incompleteFileManager;
    private Provider<RequeryManagerFactory> requeryManagerFactory;
    private Provider<AlternateLocationFactory> alternateLocationFactory;
    private Provider<DownloadCallback> downloadCallback;
    private NetworkManager networkManager;
    private Provider<QueryRequestFactory> queryRequestFactory;
    private Provider<AltLocManager> altLocManager;
    private Provider<DownloadWorkerFactory> downloadWorkerFactory;
    private Provider<OnDemandUnicaster> onDemandUnicaster;
    private Provider<UrnCache> urnCache;
    private Provider<SourceRankerFactory> sourceRankerFactory;
    private Provider<ContentManager> contentManager;
    private Provider<SavedFileManager> savedFileManager;
    private Provider<DiskController> diskController;
    private Provider<VerifyingFileFactory> verifyingFileFactory;
    private Provider<IPFilter> ipFilter;
    private Provider<TigerTreeCache> tigerTreeCache;
    private Provider<MessageRouter> messageRouter;
    private ApplicationServices applicationServices;
    private Provider<ScheduledExecutorService> backgroundExecutor;

    @Inject   
    public GnutellaDownloaderFactoryImpl(Provider<SaveLocationManager> saveLocationManager,
            Provider<FileManager> fileManager, Provider<DownloadManager> downloadManager,
            Provider<IncompleteFileManager> incompleteFileManager,
            Provider<RequeryManagerFactory> requeryManagerFactory,
            Provider<AlternateLocationFactory> alternateLocationFactory,
            Provider<DownloadCallback> downloadCallback, NetworkManager networkManager,
            Provider<QueryRequestFactory> queryRequestFactory,
            Provider<AltLocManager> altLocManager,
            Provider<DownloadWorkerFactory> downloadWorkerFactory,
            Provider<OnDemandUnicaster> onDemandUnicaster, Provider<UrnCache> urnCache,
            Provider<SourceRankerFactory> sourceRankerFactory,
            Provider<ContentManager> contentManager, Provider<SavedFileManager> savedFileManager,
            Provider<DiskController> diskController,
            Provider<VerifyingFileFactory> verifyingFileFactory, @Named("ipFilter") Provider<IPFilter> ipFilter,
            Provider<TigerTreeCache> tigerTreeCache, Provider<MessageRouter> messageRouter,
            ApplicationServices applicationServices,
            @Named("backgroundExecutor") Provider<ScheduledExecutorService> backgroundExecutor) {
        this.saveLocationManager = saveLocationManager;
        this.fileManager = fileManager;
        this.downloadManager = downloadManager;
        this.incompleteFileManager = incompleteFileManager;
        this.requeryManagerFactory = requeryManagerFactory;
        this.alternateLocationFactory = alternateLocationFactory;
        this.downloadCallback = downloadCallback;
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.altLocManager = altLocManager;
        this.downloadWorkerFactory = downloadWorkerFactory;
        this.onDemandUnicaster = onDemandUnicaster;
        this.urnCache = urnCache;
        this.sourceRankerFactory = sourceRankerFactory;
        this.contentManager = contentManager;
        this.savedFileManager = savedFileManager;
        this.diskController = diskController;
        this.verifyingFileFactory = verifyingFileFactory;
        this.ipFilter = ipFilter;
        this.tigerTreeCache = tigerTreeCache;
        this.messageRouter = messageRouter;
        this.applicationServices = applicationServices;
        this.backgroundExecutor = backgroundExecutor;
    }
    
    public ManagedDownloader createManagedDownloader(
            RemoteFileDesc[] files, GUID originalQueryGUID, File saveDirectory, String fileName,
            boolean overwrite) throws SaveLocationException {
        return new ManagedDownloader(files, originalQueryGUID,
                saveDirectory, fileName,
                overwrite, saveLocationManager.get(), downloadManager.get(), fileManager.get(),
                incompleteFileManager.get(), downloadCallback.get(), networkManager,
                alternateLocationFactory.get(), requeryManagerFactory.get(), queryRequestFactory.get(),
                onDemandUnicaster.get(), downloadWorkerFactory.get(), altLocManager.get(),
                contentManager.get(), sourceRankerFactory.get(), urnCache.get(), savedFileManager
                        .get(), verifyingFileFactory.get(), diskController.get(), ipFilter.get(),
                backgroundExecutor.get(), messageRouter, tigerTreeCache, applicationServices);
    }

    public MagnetDownloader createMagnetDownloader(
            MagnetOptions magnet, boolean overwrite,
            File saveDir, String fileName) throws SaveLocationException {
        return new MagnetDownloader(magnet, overwrite, saveDir, fileName, saveLocationManager.get(), downloadManager.get(), fileManager.get(),
                incompleteFileManager.get(), downloadCallback.get(), networkManager,
                alternateLocationFactory.get(), requeryManagerFactory.get(), queryRequestFactory.get(),
                onDemandUnicaster.get(), downloadWorkerFactory.get(), altLocManager.get(),
                contentManager.get(), sourceRankerFactory.get(), urnCache.get(), savedFileManager
                        .get(), verifyingFileFactory.get(), diskController.get(), ipFilter.get(),
                backgroundExecutor.get(), messageRouter, tigerTreeCache, applicationServices);
    }

    public InNetworkDownloader createInNetworkDownloader(
            DownloadInformation info, File dir, long startTime)
            throws SaveLocationException {
        return new InNetworkDownloader(info, dir,
                startTime, saveLocationManager.get(), downloadManager.get(), fileManager.get(),
                incompleteFileManager.get(), downloadCallback.get(), networkManager,
                alternateLocationFactory.get(), requeryManagerFactory.get(), queryRequestFactory.get(),
                onDemandUnicaster.get(), downloadWorkerFactory.get(), altLocManager.get(),
                contentManager.get(), sourceRankerFactory.get(), urnCache.get(), savedFileManager
                        .get(), verifyingFileFactory.get(), diskController.get(), ipFilter.get(),
                backgroundExecutor.get(), messageRouter, tigerTreeCache, applicationServices);
    }

    public ResumeDownloader createResumeDownloader(File incompleteFile,
            String name, long size) throws SaveLocationException {
        return new ResumeDownloader(incompleteFile,
                name, size, saveLocationManager.get(), downloadManager.get(), fileManager.get(),
                incompleteFileManager.get(), downloadCallback.get(), networkManager,
                alternateLocationFactory.get(), requeryManagerFactory.get(), queryRequestFactory.get(),
                onDemandUnicaster.get(), downloadWorkerFactory.get(), altLocManager.get(),
                contentManager.get(), sourceRankerFactory.get(), urnCache.get(), savedFileManager
                        .get(), verifyingFileFactory.get(), diskController.get(), ipFilter.get(),
                backgroundExecutor.get(), messageRouter, tigerTreeCache, applicationServices);
    }

}
