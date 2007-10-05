package com.limegroup.gnutella.downloader;

import java.io.File;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;

/**
 * A default implementation of {@link PurchasedStoreDownloaderFactory}   
 */
public class PurchasedStoreDownloaderFactoryImpl implements PurchasedStoreDownloaderFactory {

    private final Provider<SaveLocationManager> saveLocationManager;

    @Inject
    public PurchasedStoreDownloaderFactoryImpl(Provider<SaveLocationManager> saveLocationManager) {
        this.saveLocationManager = saveLocationManager;
    }
    
    public StoreDownloader createStoreDownloader(RemoteFileDesc rfd, IncompleteFileManager ifm, 
            File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException {
        return new StoreDownloader(rfd, ifm, saveDirectory, fileName, 
                overwrite, saveLocationManager.get());
    }
}
