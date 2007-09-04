package com.limegroup.gnutella.downloader;

import java.io.File;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.version.DownloadInformation;
import com.limegroup.store.StoreDescriptor;

public interface GnutellaDownloaderFactory {

    public abstract ManagedDownloader createManagedDownloader(
            RemoteFileDesc[] files, IncompleteFileManager ifc,
            GUID originalQueryGUID, File saveDirectory, String fileName,
            boolean overwrite) throws SaveLocationException;

    public abstract ManagedDownloader createManagedDownloader(
            RemoteFileDesc[] files, IncompleteFileManager ifc,
            GUID originalQueryGUID);

    public abstract MagnetDownloader createMagnetDownloader(
            IncompleteFileManager ifm, MagnetOptions magnet, boolean overwrite,
            File saveDir, String fileName) throws SaveLocationException;
    
    public abstract StoreDownloader createStoreDownloader(
            StoreDescriptor store, IncompleteFileManager ifm, 
            File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException;

    public abstract InNetworkDownloader createInNetworkDownloader(
            IncompleteFileManager incompleteFileManager,
            DownloadInformation info, File dir, long startTime)
            throws SaveLocationException;

    public abstract ResumeDownloader createResumeDownloader(
            IncompleteFileManager incompleteFileManager, File incompleteFile,
            String name, long size);

}
