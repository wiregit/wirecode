package org.limewire.core.api.download;


import java.io.File;
import java.net.URI;

import org.limewire.core.api.URN;
import org.limewire.core.api.magnet.MagnetLink;

import ca.odell.glazedlists.EventList;

public interface DownloadListManager extends ResultDownloader {
	
    /**
     * Returns all items currently being downloaded.
     */
	public EventList<DownloadItem> getDownloads();
	
	/** Returns a Swing-thread safe version of the downloads event list. */
	public EventList<DownloadItem> getSwingThreadSafeDownloads();
    
	/**
	 * Downloads the torrent file at the given uri.
	 */
    public DownloadItem addTorrentDownload(URI uri, boolean overwrite) throws SaveLocationException;

    /**
     * Opens the given file and starts a downloader based on the information inside of the given file.
     */
    public DownloadItem addTorrentDownload(File file, File saveFile, boolean overwrite) throws SaveLocationException;

	/**
	 * Return true if the downloader contains the given urn, false otherwise
	 */
    public boolean contains(URN urn);

    /**
     * Downloads the given magnet link.
     */
    public DownloadItem addDownload(MagnetLink magnet, File saveFile, boolean overwrite) throws SaveLocationException;
}
