package org.limewire.core.api.download;


import ca.odell.glazedlists.EventList;

public interface DownloadListManager extends ResultDownloader {
	
    /**
     * Returns all items currently being downloaded.
     */
	public EventList<DownloadItem> getDownloads();
	
	/** Returns a Swing-thread safe version of the downloads event list. */
	public EventList<DownloadItem> getSwingThreadSafeDownloads();
    
    
	
	
}
