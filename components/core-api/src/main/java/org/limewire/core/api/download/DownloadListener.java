package org.limewire.core.api.download;

import com.limegroup.gnutella.Downloader;

public interface DownloadListener {
    
    public void downloadAdded(Downloader downloader);

    public void downloadRemoved(Downloader downloader);
}
