package org.limewire.core.impl.download.listener;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.limewire.bittorrent.TorrentManager;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.settings.SharingSettings;
import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.bittorrent.BTTorrentFileDownloader;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;

/**
 * Listens for downloads of .torrent files to complete. When the download
 * finishes then the torrent download will be started.
 */
public class TorrentDownloadListener implements EventListener<DownloadStateEvent> {

    private final Downloader downloader;
    private final DownloadManager downloadManager;
    private final ActivityCallback activityCallback;
    private final List<DownloadItem> downloadItems;
    private final TorrentManager torrentManager;

    @Inject
    public TorrentDownloadListener(DownloadManager downloadManager,
            ActivityCallback activityCallback, TorrentManager torrentManager,
            @Assisted List<DownloadItem> downloadItems, @Assisted Downloader downloader) {
        this.downloader = Objects.nonNull(downloader, "downloader");
        this.downloadManager = Objects.nonNull(downloadManager, "downloadManager");
        this.torrentManager = Objects.nonNull(torrentManager, "torrentManager");
        this.activityCallback = Objects.nonNull(activityCallback, "activityCallback");
        this.downloadItems = Objects.nonNull(downloadItems, "downloadItems");

        if (downloader.getState() == DownloadState.COMPLETE) {
            if (downloader instanceof CoreDownloader) {
                handleEvent(new DownloadStateEvent((CoreDownloader) downloader,
                        DownloadState.COMPLETE));
            }
        }
    }

    @Override
    public void handleEvent(DownloadStateEvent event) {
        DownloadState downloadStatus = event.getType();
        if (DownloadState.COMPLETE == downloadStatus) {
            if (downloader instanceof BTTorrentFileDownloader) {
                handleBTTorrentFileDownloader();
            } else {
                handleCoreDownloader();
            }
        }
    }

    private void handleCoreDownloader() {
        File possibleTorrentFile = null;

        possibleTorrentFile = downloader.getSaveFile();
        String fileExtension = FileUtils.getFileExtension(possibleTorrentFile);
        if ("torrent".equalsIgnoreCase(fileExtension)) {
            try {
                downloadManager.downloadTorrent(possibleTorrentFile, null, false);
                downloadItems.remove(getDownloadItem(downloader));
            } catch (DownloadException e) {
                final File torrentFile = possibleTorrentFile;
                activityCallback.handleDownloadException(new DownloadAction() {
                    @Override
                    public void download(File saveDirectory, boolean overwrite)
                            throws DownloadException {
                        downloadManager.downloadTorrent(torrentFile, saveDirectory, overwrite);
                        downloadItems.remove(getDownloadItem(downloader));
                    }

                    @Override
                    public void downloadCanceled(DownloadException ignored) {
                        // nothing to do
                    }

                }, e, false);
            }
        }
    }

    private void handleBTTorrentFileDownloader() {
        File torrentCopy = null;
        File torrentFile = null;
        final BTTorrentFileDownloader btTorrentFileDownloader = (BTTorrentFileDownloader) downloader;
        try {
            torrentFile = btTorrentFileDownloader.getTorrentFile();
            torrentCopy = new File(SharingSettings.INCOMPLETE_DIRECTORY.get(), UUID.randomUUID()
                    .toString()
                    + ".torrent");
            //copy used to handle certain exception cases where the 
            //old torrent file may have been removed because of logic to 
            //clean up the downloaders. This is because downloadTorrent, will
            //call the deleteIncomplete files method for the downloader and 
            //the only copy is the one in the incomplete files directory.
            //by keeping a copy around we can continue the download if needed.
            FileUtils.copy(torrentFile, torrentCopy);
            downloadManager.downloadTorrent(torrentFile, null, false);
            downloadItems.remove(getDownloadItem(downloader));
        } catch (DownloadException e) {
            final File torrentFileFinal = torrentFile;
            final File torrentCopyFinal = torrentCopy;
            activityCallback.handleDownloadException(new DownloadAction() {
                @Override
                public void download(File saveDirectory, boolean overwrite)
                        throws DownloadException {
                    FileUtils.copy(torrentCopyFinal, torrentFileFinal);
                    downloadManager.downloadTorrent(torrentFileFinal, saveDirectory, overwrite);
                    downloadItems.remove(getDownloadItem(downloader));
                    FileUtils.forceDelete(torrentCopyFinal);
                }

                @Override
                public void downloadCanceled(DownloadException ex) {
                    if (!torrentManager.isDownloadingTorrent(torrentFileFinal)) {
                        // need to delete to clean up the torrent file in the
                        // incomplete directory
                        FileUtils.forceDelete(torrentFileFinal);
                    }
                    FileUtils.forceDelete(torrentCopyFinal);
                }

            }, e, false);
        }
    }

    DownloadItem getDownloadItem(Downloader downloader) {
        DownloadItem item = (DownloadItem) downloader.getAttribute(DownloadItem.DOWNLOAD_ITEM);
        return item;
    }
}