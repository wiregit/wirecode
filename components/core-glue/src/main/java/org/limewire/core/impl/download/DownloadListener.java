package org.limewire.core.impl.download;

import com.limegroup.gnutella.Downloader;


public interface DownloadListener {
    
    public void downloadAdded(Downloader downloader);

    public void downloadRemoved(Downloader downloader);
}
