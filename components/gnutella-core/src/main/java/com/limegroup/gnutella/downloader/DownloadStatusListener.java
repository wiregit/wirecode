package com.limegroup.gnutella.downloader;

import org.limewire.listener.Event;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.Downloader.DownloadStatus;

public interface DownloadStatusListener extends EventListener<Event<CoreDownloader, DownloadStatus>> {

}
