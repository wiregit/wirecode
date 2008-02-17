package com.limegroup.gnutella.downloader;

import org.limewire.listener.Event;
import org.limewire.listener.EventListener;

public interface DownloadManagerListener extends EventListener<Event<CoreDownloader, DownloadManagerEvent>>{
    
}
