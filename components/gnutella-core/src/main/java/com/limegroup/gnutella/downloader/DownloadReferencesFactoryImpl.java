package com.limegroup.gnutella.downloader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.messages.QueryRequestFactory;

@Singleton
// DPINJ:  Get rid of this!  See: CORE-306
public class DownloadReferencesFactoryImpl implements DownloadReferencesFactory {

    private final Provider<DownloadManager> downloadManager;
    private final Provider<FileManager> fileManager;
    private final Provider<DownloadCallback> downloadCallback;
    private final Provider<DownloadCallback> innetworkCallback;
    private final Provider<NetworkManager> networkManager;
    private final Provider<AlternateLocationFactory> alternateLocationFactory;
    private final Provider<QueryRequestFactory> queryRequestFactory;
    private final Provider<OnDemandUnicaster> onDemandUnicaster;
    private final Provider<DownloadWorkerFactory> downloadWorkerFactory;
    private final Provider<ManagedTorrentFactory> managedTorrentFactory;
    private final Provider<AltLocManager> altLocManager;
    private final Provider<ContentManager> contentManager;
    private final Provider<SourceRankerFactory> sourceRankerFactory;
    private final Provider<UrnCache> urnCache;
    private final Provider<SavedFileManager> savedFileManager;
    private final Provider<VerifyingFileFactory> verifyingFileFactory;
    private final Provider<DiskController> diskController;
    private final Provider<IPFilter> ipFilter;
    private final Provider<RequeryManagerFactory> requeryManagerFactory;
    
    @Inject
    public DownloadReferencesFactoryImpl(
            Provider<DownloadManager> downloadManager,
            Provider<FileManager> fileManager,
            Provider<DownloadCallback> downloadCallback,
            @Named("inNetwork") Provider<DownloadCallback> innetworkCallback,
            Provider<NetworkManager> networkManager,
            Provider<AlternateLocationFactory> alternateLocationFactory,
            Provider<QueryRequestFactory> queryRequestFactory,
            Provider<OnDemandUnicaster> onDemandUnicaster,
            Provider<DownloadWorkerFactory> downloadWorkerFactory,
            Provider<ManagedTorrentFactory> managedTorrentFactory,
            Provider<AltLocManager> altLocManager,
            Provider<ContentManager> contentManager,
            Provider<SourceRankerFactory> sourceRankerFactory,
            Provider<UrnCache> urnCache,
            Provider<SavedFileManager> savedFileManager,
            Provider<VerifyingFileFactory> verifyingFileFactory,
            Provider<DiskController> diskController,
            Provider<IPFilter> ipFilter,
            Provider<RequeryManagerFactory> requeryManagerFactory) {
        this.downloadManager = downloadManager;
        this.fileManager = fileManager;
        this.downloadCallback = downloadCallback;
        this.innetworkCallback = innetworkCallback;
        this.networkManager = networkManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.queryRequestFactory = queryRequestFactory;
        this.onDemandUnicaster = onDemandUnicaster;
        this.downloadWorkerFactory = downloadWorkerFactory;
        this.managedTorrentFactory = managedTorrentFactory;
        this.altLocManager = altLocManager;
        this.contentManager = contentManager;
        this.sourceRankerFactory = sourceRankerFactory;
        this.urnCache = urnCache;
        this.savedFileManager = savedFileManager;
        this.verifyingFileFactory = verifyingFileFactory;
        this.diskController = diskController;
        this.ipFilter = ipFilter;
        this.requeryManagerFactory = requeryManagerFactory;
    }

    public DownloadReferences create(Downloader downloader) {
        return new DownloadReferences(downloadManager.get(), fileManager.get(),
                downloader instanceof InNetworkDownloader ? innetworkCallback
                        .get() : downloadCallback.get(), networkManager.get(),
                alternateLocationFactory.get(), queryRequestFactory.get(),
                onDemandUnicaster.get(), downloadWorkerFactory.get(),
                managedTorrentFactory.get(), altLocManager.get(),
                contentManager.get(), sourceRankerFactory.get(),
                urnCache.get(), savedFileManager.get(), verifyingFileFactory
                        .get(), diskController.get(), ipFilter.get(),
                requeryManagerFactory.get());
    }

}
