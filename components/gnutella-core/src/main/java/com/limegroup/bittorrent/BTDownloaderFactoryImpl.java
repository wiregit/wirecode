package com.limegroup.bittorrent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.SaveLocationManager;

@Singleton
public class BTDownloaderFactoryImpl implements BTDownloaderFactory {
    
    private final BTContextFactory btContextFactory;
    private final Provider<SaveLocationManager> saveLocationManager;

    @Inject
    public BTDownloaderFactoryImpl(BTContextFactory btContextFactory, Provider<SaveLocationManager> saveLocationManager) {
        this.btContextFactory = btContextFactory;
        this.saveLocationManager = saveLocationManager;
    }

    public BTDownloader createBTDownloader(BTMetaInfo info) {
        return new BTDownloader(info, btContextFactory, saveLocationManager.get());
    }
}
