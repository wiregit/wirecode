package org.limewire.core.api.download;


import ca.odell.glazedlists.EventList;

public interface DownloadListManager {
	
	public EventList<DownloadItem> getDownloads();
	
	
}
