package com.limegroup.gnutella.downloader;

import java.io.File;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;

/**
 *  Provides methods for generating downloads of purchased content
 */
public interface PurchasedStoreDownloaderFactory {

    /**
     * 
     * @param rfd - location to download from
     * @param ifm - manager that handles the file while its being download
     * @param saveDirectory - location to save the file
     * @param fileName - name of file once download is complete
     * @param overwrite - true to overwrite a file with the same name in the same directory
     * @return - a StoreDownloader to begin downloading from
     * @throws SaveLocationException
     */
    public abstract StoreDownloader createStoreDownloader(
            RemoteFileDesc rfd, IncompleteFileManager ifm, 
            File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException;
    
}
