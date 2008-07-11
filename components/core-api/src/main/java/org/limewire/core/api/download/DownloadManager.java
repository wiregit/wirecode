package org.limewire.core.api.download;

import java.util.List;

public interface DownloadManager {

	public void addDownloadAddedListener(DownloadAddedListener listener);
	public void removeDownloadAddedListener(DownloadAddedListener listener);
	
	public List<DownloadItem> getDownloads();
	
	
}
