package com.limegroup.gnutella.downloader;

import org.limewire.bittorrent.Torrent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.version.UpdateHandler;

/**
 * Once an in-network download finishes, the UpdateHandler is notified.
 */
@Singleton
public class InNetworkCallback implements DownloadCallback {
    
    private final UpdateHandler updateHandler;

    @Inject
    public InNetworkCallback(UpdateHandler updateHandler) {
        this.updateHandler = updateHandler;
    }
    
    @Override
    public void addDownload(Downloader d) {
    }

    @Override
    public void removeDownload(Downloader d) {
        InNetworkDownloader downloader = (InNetworkDownloader) d;
        updateHandler.inNetworkDownloadFinished(
                downloader.getSha1Urn(),
                downloader.getState() == DownloadState.COMPLETE);
    }

    @Override
    public void downloadsComplete() {
    }

    // always discard corruption.
    @Override
    public void promptAboutCorruptDownload(Downloader dloader) {
        dloader.discardCorruptDownload(true);
    }
    
    @Override
    public void promptAboutUnscannedPreview(Downloader dloader) {
        dloader.discardUnscannedPreview(true);
    }

    @Override
    public boolean promptTorrentFilePriorities(Torrent torrent) {
        return true;
    }
}