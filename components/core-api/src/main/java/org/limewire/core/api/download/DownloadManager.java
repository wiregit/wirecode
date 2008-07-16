package org.limewire.core.api.download;


import ca.odell.glazedlists.EventList;

public interface DownloadManager {

	public void addDownloadAddedListener(DownloadAddedListener listener);
	public void removeDownloadAddedListener(DownloadAddedListener listener);
	
	public EventList<DownloadItem> getDownloads();
	
	
}
