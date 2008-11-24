package org.limewire.core.impl.download.listener;

import java.io.File;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.listener.EventListener;
import org.limewire.util.Objects;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;

/**
 * Listens for the completion of downloads, adding completed downloads to the DownloadSettings.RECENT_DOWNLOADS list.
 */
public class RecentDownloadListener implements EventListener<DownloadStatusEvent> {
    private final Downloader downloader;
    public RecentDownloadListener(Downloader downloader) {
        this.downloader = Objects.nonNull(downloader, "downloader");
        if(downloader.getState() == DownloadStatus.COMPLETE) {
            if(downloader instanceof CoreDownloader) {
                handleEvent(new DownloadStatusEvent((CoreDownloader)downloader, DownloadStatus.COMPLETE));
            }
        }
    }
    @Override
    public void handleEvent(DownloadStatusEvent event) {
        //TODO don't do anything for torrent downloads?
        DownloadStatus downloadStatus = event.getType();
        if(DownloadStatus.COMPLETE == downloadStatus) {
            File saveFile = downloader.getSaveFile();
            if(saveFile != null) {
                if(DownloadSettings.REMEMBER_RECENT_DOWNLOADS.getValue()) {
                    DownloadSettings.RECENT_DOWNLOADS.add(saveFile);
                }
            }
        }
    }
}