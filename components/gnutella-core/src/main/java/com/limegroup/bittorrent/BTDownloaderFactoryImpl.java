package com.limegroup.bittorrent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.SaveLocationManager;

@Singleton
public class BTDownloaderFactoryImpl implements BTDownloaderFactory {
    
    private final BTContextFactory btContextFactory;
    private final Provider<SaveLocationManager> saveLocationManager;
    private final BTUploaderFactory btUploaderFactory;
    private final Provider<TorrentManager> torrentManager;
    private final Provider<ManagedTorrentFactory> managedTorrentFactory;
    private final Provider<DownloadManager> downloadManager;

    @Inject
    public BTDownloaderFactoryImpl(BTContextFactory btContextFactory,
            Provider<SaveLocationManager> saveLocationManager,
            Provider<TorrentManager> torrentManager, BTUploaderFactory btUploaderFactory,
            Provider<ManagedTorrentFactory> managedTorrentFactory,
            Provider<DownloadManager> downloadManager) {
        this.btContextFactory = btContextFactory;
        this.saveLocationManager = saveLocationManager;
        this.torrentManager = torrentManager;
        this.btUploaderFactory = btUploaderFactory;
        this.managedTorrentFactory = managedTorrentFactory;
        this.downloadManager = downloadManager;
    }

    public BTDownloader createBTDownloader(BTMetaInfo info) {
        return new BTDownloader(info, btContextFactory, saveLocationManager.get(), torrentManager,
                btUploaderFactory, downloadManager.get(), managedTorrentFactory.get());
    }
    
}
