package com.limegroup.gnutella.downloader;

import org.limewire.listener.DefaultEvent;
import org.limewire.listener.EventListener;

public interface DownloadManagerListener extends EventListener<DefaultEvent<CoreDownloader, DownloadManagerEvent>>{
    
}
