package com.limegroup.gnutella.downloader;

import java.io.File;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;

public interface PurchasedStoreDownloaderFactory {

    public abstract StoreDownloader createStoreDownloader(
            RemoteFileDesc rfd, IncompleteFileManager ifm, 
            File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException;
    
}
