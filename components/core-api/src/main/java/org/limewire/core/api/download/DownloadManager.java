package org.limewire.core.api.download;


import ca.odell.glazedlists.EventList;

public interface DownloadManager {
	
	public EventList<DownloadItem> getDownloads();
	
	
}
