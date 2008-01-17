package com.limegroup.gnutella.downloader;

import java.io.File;

import org.limewire.io.InvalidDataException;

import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.version.DownloadInformation;

/**
 * Constructs all kinds of {@link CoreDownloader CoreDownloaders}.<p>
 * 
 * This handles creating downloads from data as well as from mementos
 * of prior downloads.
 */
public interface CoreDownloaderFactory {

    public ManagedDownloader createManagedDownloader(RemoteFileDesc[] files,
            GUID originalQueryGUID, File saveDirectory, String fileName, boolean overwrite)
            throws SaveLocationException;

    public MagnetDownloader createMagnetDownloader(MagnetOptions magnet, boolean overwrite,
            File saveDir, String fileName) throws SaveLocationException;

    public InNetworkDownloader createInNetworkDownloader(DownloadInformation info, File dir,
            long startTime) throws SaveLocationException;

    public ResumeDownloader createResumeDownloader(File incompleteFile, String name, long size)
            throws SaveLocationException;

    /**
     * @param rfd - location to download from
     * @param saveDirectory - location to save the file
     * @param fileName - name of file once download is complete
     * @param overwrite - true to overwrite a file with the same name in the
     *        same directory
     * @return - a StoreDownloader to begin downloading from
     * @throws SaveLocationException
     */
    public StoreDownloader createStoreDownloader(RemoteFileDesc rfd, File saveDirectory,
            String fileName, boolean overwrite) throws SaveLocationException;
    
    public BTDownloader createBTDownloader(BTMetaInfo info);

    /**
     * Creates the appropriate kind of downloader from a given DownloadMemento.
     */
    public CoreDownloader createFromMemento(DownloadMemento memento) throws InvalidDataException; 

}
