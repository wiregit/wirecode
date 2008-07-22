package org.limewire.core.api.download;


import ca.odell.glazedlists.EventList;

public interface DownloadListManager extends SearchResultDownloader {
	
    /**
     * Returns all items currently being downloaded.
     */
	public EventList<DownloadItem> getDownloads();
    
    
	
	
}
