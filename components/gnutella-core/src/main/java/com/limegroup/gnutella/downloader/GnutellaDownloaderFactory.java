package com.limegroup.gnutella.downloader;

import java.io.File;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.version.DownloadInformation;

public interface GnutellaDownloaderFactory {

    public abstract ManagedDownloader createManagedDownloader(
            RemoteFileDesc[] files, GUID originalQueryGUID, File saveDirectory, String fileName,
            boolean overwrite) throws SaveLocationException;

    public abstract MagnetDownloader createMagnetDownloader(
            MagnetOptions magnet, boolean overwrite,
            File saveDir, String fileName) throws SaveLocationException;

    public abstract InNetworkDownloader createInNetworkDownloader(
            DownloadInformation info, File dir, long startTime)
            throws SaveLocationException;

    public abstract ResumeDownloader createResumeDownloader(
            File incompleteFile,
            String name, long size) throws SaveLocationException;

}
