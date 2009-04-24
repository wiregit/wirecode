/**
 * 
 */
package org.limewire.libtorrent;

import java.io.File;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.Downloader.DownloadState;

public class Torrent {

    private final TorrentManager torrentManager;

    private final AtomicReference<LibTorrentStatus> status;

    private LibTorrentInfo info = null;

    public Torrent(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
        this.status = new AtomicReference<LibTorrentStatus>();
    }

    public synchronized void init(File torrent) {
        LibTorrentInfo info = torrentManager.addTorrent(torrent);
        setInfo(info);

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
        LibTorrentStatus status = this.status.get();
        return status == null ? 0 : status.download_rate;
    }

    public void cancel() {
        torrentManager.removeTorrent(getSha1());
    }

    public String getSha1() {
        return info == null ? null : info.sha1;
    }

    public boolean isPaused() {
        LibTorrentStatus status = this.status.get();
        return status == null ? false : status.paused;
    }

    public DownloadState getState() {
        LibTorrentStatus status = this.status.get();
        if (status == null) {
            return DownloadState.QUEUED;
        }
        LibTorrentState state = LibTorrentState.forId(status.state);
        return convertState(state);
    }

    public boolean isFinished() {
        LibTorrentStatus status = this.status.get();
        return status == null ? false : status.finished;
    }

    public long getTotalSize() {
        BigInteger size = new BigInteger(info.content_length);

        return size.longValue();
    }

    public long getTotalDownloaded() {
        LibTorrentStatus status = this.status.get();
        if (status == null) {
            return 0;
        } else {
            BigInteger done = new BigInteger(status.total_done);
            return done.longValue();
        }
    }

    public int getNumPeers() {
        LibTorrentStatus status = this.status.get();
        return status == null ? 0 : status.num_peers;
    }

    public int getPieceLength() {
        return info == null ? -1 : info.piece_length;
    }

    public synchronized void setInfo(LibTorrentInfo info) {
        this.info = info;
    }

    public synchronized void setStatus(LibTorrentStatus status) {
        this.status.set(status);
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