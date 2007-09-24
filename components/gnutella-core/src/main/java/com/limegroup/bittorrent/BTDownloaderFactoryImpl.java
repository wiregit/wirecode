package com.limegroup.bittorrent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.SaveLocationManager;

@Singleton
public class BTDownloaderFactoryImpl implements BTDownloaderFactory {
    
    private final BTContextFactory btContextFactory;
    private final Provider<SaveLocationManager> saveLocationManager;
    private final BTUploaderFactory btUploaderFactory;
    private final Provider<TorrentManager> torrentManager;

    @Inject
    public BTDownloaderFactoryImpl(BTContextFactory btContextFactory, Provider<SaveLocationManager> saveLocationManager, Provider<TorrentManager> torrentManager, BTUploaderFactory btUploaderFactory) {
        this.btContextFactory = btContextFactory;
        this.saveLocationManager = saveLocationManager;
        this.torrentManager = torrentManager;
        this.btUploaderFactory = btUploaderFactory;
    }

    public BTDownloader createBTDownloader(BTMetaInfo info) {
        return new BTDownloader(info, btContextFactory, saveLocationManager.get(), torrentManager, btUploaderFactory);
    }
    
}
