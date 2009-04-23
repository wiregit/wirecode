/**
 * 
 */
package org.limewire.libtorrent;

import java.io.File;
import java.math.BigInteger;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.Downloader.DownloadState;

public class Torrent {

    private final TorrentManager torrentManager;

    private LibTorrentInfo info = null;

    private LibTorrentStatus status = null;

    public Torrent(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
    }

    public synchronized void init(File torrent) {
        LibTorrentInfo info = torrentManager.addTorrent(torrent);
        setInfo(info);

        System.out.println(info);
        
        LibTorrentStatus status = torrentManager.getStatus(info.sha1);
        setStatus(status);

        torrentManager.addListener(info.sha1, new EventListener<LibTorrentEvent>() {
            public void handleEvent(LibTorrentEvent event) {
                LibTorrentStatus status = event.getTorrentStatus();
                setStatus(status);
            }
        });
    }

    public void pause() {
        torrentManager.pauseTorrent(getSha1());
    }

    public void resume() {
        torrentManager.resumeTorrent(getSha1());
    }

    public float getDownloadRate() {
        return status == null ? 0 : status.download_rate;
    }

    public void cancel() {
        torrentManager.removeTorrent(getSha1());
    }

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
        BigInteger size = new BigInteger(info.content_length);
        
        return size.longValue();
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