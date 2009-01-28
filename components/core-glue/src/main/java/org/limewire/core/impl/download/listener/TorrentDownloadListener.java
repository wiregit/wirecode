package org.limewire.core.impl.download.listener;

import java.io.File;
import java.util.List;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.BTTorrentFileDownloader;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;

/**
 * Listens for downloads of .torrent files to complete. When the download
 * finishes then the torrent download will be started.
 */
public class TorrentDownloadListener implements EventListener<DownloadStatusEvent> {
    
    private final Downloader downloader;

    private final DownloadManager downloadManager;

    private final ActivityCallback activityCallback;

    private final List<DownloadItem> downloadItems;

    public TorrentDownloadListener(DownloadManager downloadManager, ActivityCallback activityCallback,
            List<DownloadItem> downloadItems, Downloader downloader) {
        this.downloader = Objects.nonNull(downloader, "downloader");
        this.downloadManager = Objects.nonNull(downloadManager, "downloadManager");
        this.activityCallback = Objects.nonNull(activityCallback, "activityCallback");
        this.downloadItems = Objects.nonNull(downloadItems, "downloadItems");

        if (downloader.getState() == DownloadStatus.COMPLETE) {
            // TODO not sure why Downloader and CoreDownloader are not merged
            // into one class.
            // No classes implement Downloader, CoreDownloader extends
            // Downloader, and all downloaders implement CoreDownloader
            if (downloader instanceof CoreDownloader) {
                handleEvent(new DownloadStatusEvent((CoreDownloader) downloader,
                        DownloadStatus.COMPLETE));
            }
        }
    }

    @Override
    public void handleEvent(DownloadStatusEvent event) {
        DownloadStatus downloadStatus = event.getType();
        if (DownloadStatus.COMPLETE == downloadStatus) {
            if (downloader instanceof BTTorrentFileDownloader) {
                BTMetaInfo btMetaInfo = null;
                try {
                    BTTorrentFileDownloader btTorrentFileDownloader = (BTTorrentFileDownloader) downloader;
                    btMetaInfo = btTorrentFileDownloader.getBtMetaInfo();
                    downloadItems.remove(getDownloadItem(downloader));
                    downloadManager.downloadTorrent(btMetaInfo, true);
                } catch (SaveLocationException sle) {
                    final BTMetaInfo btMetaInfoCopy = btMetaInfo;
                    activityCallback.handleSaveLocationException(new DownloadAction() {
                        @Override
                        public void download(File saveFile, boolean overwrite)
                                throws SaveLocationException {
                            downloadItems.remove(getDownloadItem(downloader));
                            downloadManager.downloadTorrent(btMetaInfoCopy, overwrite);
                        }
                    }, sle, false);
                }
            } else {
                File possibleTorrentFile = null;
                try {
                    possibleTorrentFile = downloader.getSaveFile();
                    String fileExtension = FileUtils.getFileExtension(possibleTorrentFile);
                    if ("torrent".equalsIgnoreCase(fileExtension)) {
                        downloadItems.remove(getDownloadItem(downloader));
                        downloadManager.downloadTorrent(possibleTorrentFile, false);
                    }
                } catch (SaveLocationException sle) {
                    final File torrentFile = possibleTorrentFile;
                    activityCallback.handleSaveLocationException(new DownloadAction() {
                        @Override
                        public void download(File saveFile, boolean overwrite)
                                throws SaveLocationException {
                            downloadItems.remove(getDownloadItem(downloader));
                            downloadManager.downloadTorrent(torrentFile, overwrite);
                        }
                    }, sle, false);
                }
            }
        }
    }

    DownloadItem getDownloadItem(Downloader downloader) {
        DownloadItem item = (DownloadItem) downloader.getAttribute(DownloadItem.DOWNLOAD_ITEM);
        return item;
    }
}