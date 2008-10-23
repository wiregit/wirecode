package org.limewire.core.api.download;


import java.io.File;
import java.net.URI;

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
     */
    public DownloadItem addDownload(File file) throws SaveLocationException;

	
	
}
