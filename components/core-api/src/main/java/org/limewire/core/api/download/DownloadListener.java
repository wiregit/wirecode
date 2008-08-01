package org.limewire.core.api.download;


public interface DownloadListener {
    
    public void downloadAdded(DownloadItem downloadItem);

    public void downloadRemoved(DownloadItem downloadItem);
}
