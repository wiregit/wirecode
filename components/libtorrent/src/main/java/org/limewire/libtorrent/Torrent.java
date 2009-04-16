/**
 * 
 */
package org.limewire.libtorrent;


import com.limegroup.gnutella.Downloader.DownloadState;

public class Torrent {
    private LibTorrentInfo info = null;

    private LibTorrentStatus status = null;

    public synchronized String getSha1() {
        return info.sha1;
    }

    public synchronized boolean isPaused() {
        return status == null ? false : status.paused;
    }

    public synchronized DownloadState getState() {
        if (status == null) {
            return DownloadState.QUEUED;
        }
        LibTorrentState state = LibTorrentState.forId(status.state);
        return convertState(state);
    }

    public synchronized boolean isFinished() {
        return status == null ? false : status.finished;
    }

    public synchronized long getTotalSize() {
        return info.content_length.longValue();
    }

    public synchronized long getTotalDownloaded() {
        return status == null ? 0 : status.total_done.longValue();
    }

    public synchronized int getNumPeers() {
        return status == null ? 0 : status.num_peers;
    }

    public synchronized int getPieceLength() {
        return info.piece_length;
    }

    public synchronized void setInfo(LibTorrentInfo info) {
        this.info = info;
    }

    public synchronized void setStatus(LibTorrentStatus status) {
        this.status = status;
    }

    private DownloadState convertState(LibTorrentState state) {
        switch (state) {
        case downloading:
            if (isPaused()) {
                return DownloadState.PAUSED;
            } else {
                return DownloadState.DOWNLOADING;
            }
        case queued_for_checking:
            return DownloadState.RESUMING;
        case checking_files:
            return DownloadState.RESUMING;
        case seeding:
            return DownloadState.COMPLETE;
        case finished:
            return DownloadState.COMPLETE;
        case allocating:
            return DownloadState.CONNECTING;
        case downloading_metadata:
            return DownloadState.CONNECTING;
        default:
            throw new IllegalStateException("Unknown libtorrent state: " + state);
        }
    }
}