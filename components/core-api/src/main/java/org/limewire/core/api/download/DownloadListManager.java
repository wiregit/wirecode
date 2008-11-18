package org.limewire.core.api.download;


import java.io.File;
import java.net.URI;

import org.limewire.core.api.URN;

import ca.odell.glazedlists.EventList;

public interface DownloadListManager extends ResultDownloader {
	
    /**
     * Returns all items currently being downloaded.
     */
	public EventList<DownloadItem> getDownloads();
	
	/** Returns a Swing-thread safe version of the downloads event list. */
	public EventList<DownloadItem> getSwingThreadSafeDownloads();
    
	/**
	 * Downloads the file at the given uri by selecting the proper downloader for the file.
	 */
    public DownloadItem addDownload(URI uri) throws SaveLocationException;

    /**
     * Opens the given file and starts a downloader based on the information inside of the file.
     * @param file torrent or magnet file to download.
     */
    public DownloadItem addDownload(File file) throws SaveLocationException;
    
    /**
     * Opens the given file and starts a downloader based on the information inside of the given file.
     * 
     * @param file torrent or magnet file to download.
     * @param saveFile optional filename to save the file as. does nothing for torrent downloads
     * @param overwrite whether or not to overwrite preexisting downloads with the same name
     */
    public DownloadItem addDownload(File file, File saveFile, boolean overwrite) throws SaveLocationException;

	/**
	 * Return true if the downloader contains the given urn, false otherwise
	 * @param urn
	 * @return
	 */
    public boolean contains(URN urn);
}
