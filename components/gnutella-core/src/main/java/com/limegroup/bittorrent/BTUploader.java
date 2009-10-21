package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentState;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.concurrent.ManagedThread;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.uploader.UploadType;

/**
 * Wraps the Torrent class in the Uplaoder interface to enable the gui to treat
 * the torrent uploader as a normal uploader.
 */
public class BTUploader implements Uploader, EventListener<TorrentEvent> {

    private final ActivityCallback activityCallback;

    private final Torrent torrent;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final AtomicBoolean finished = new AtomicBoolean(false);

    private volatile URN urn = null;

    private final TorrentUploadManager torrentUploadManager;

    private final TorrentManager torrentManager;

    public BTUploader(Torrent torrent, ActivityCallback activityCallback,
            TorrentUploadManager torrentUploadManager, TorrentManager torrentManager) {
        this.torrent = torrent;
        this.activityCallback = activityCallback;
        this.torrentUploadManager = torrentUploadManager;
        this.torrentManager = torrentManager;
    }

    public void registerTorrentListener() {
        torrent.addListener(this);
    }

    @Override
    public void handleEvent(TorrentEvent event) {
        if (event == TorrentEvent.STOPPED) {
            if (!finished.get()) {
                cancel();
            } else {
                remove();
            }
        } else if (event == TorrentEvent.STATUS_CHANGED) {
            // considered to be finished uploading if seed ratio has been
            // reached
            boolean finished = torrent.isFinished();
            float seedRatio = torrent.getSeedRatio();
            TorrentStatus status = torrent.getStatus();
            int seedTime = status != null ? status.getSeedingTime() : 0;

            float targetSeedRatio = torrentManager.getTorrentManagerSettings().getSeedRatioLimit();
            int targetSeedTime = torrentManager.getTorrentManagerSettings().getSeedTimeLimit();

            if (finished && (seedRatio >= targetSeedRatio || seedTime >= targetSeedTime)) {
                this.finished.set(true);
                torrent.stop();
            }
        }
    }

    private void cancel() {
        cancelled.set(true);
        remove();
    }

    private void remove() {
        torrent.removeListener(this);
        torrentUploadManager.removeMemento(torrent);
        activityCallback.removeUpload(this);
    };

    @Override
    public void stop() {
        // TODO refactor to prompt from the gui
        if (activityCallback.promptTorrentUploadCancel(torrent)) {
            new ManagedThread(new Runnable() {
                @Override
                public void run() {
                    torrent.stop();
                }
            }, "BTUploader Stop Torrent").start();
            cancel();
        }
    }

    @Override
    public String getFileName() {
        return torrent.getName();
    }

    @Override
    public long getFileSize() {
        TorrentStatus status = torrent.getStatus();
        long fileSize = status != null ? status.getTotalWanted() : -1;
        return fileSize;
    }

    @Override
    public FileDesc getFileDesc() {
        return null;
    }

    @Override
    public int getIndex() {
        // negative will make sure it never conflicts with regular uploads
        return 0 - Math.abs(hashCode());
    }

    @Override
    public long amountUploaded() {
        return torrent.getTotalUploaded();
    }

    @Override
    public long getTotalAmountUploaded() {
        return torrent.getTotalUploaded();
    }

    @Override
    public String getHost() {
        return BITTORRENT_UPLOAD;
    }

    @Override
    public UploadStatus getState() {
        if (cancelled.get()) {
            return UploadStatus.CANCELLED;
        }

        if (finished.get()) {
            return UploadStatus.COMPLETE;
        }

        TorrentStatus status = torrent.getStatus();

        if (status == null) {
            return UploadStatus.CONNECTING;
        }

        if (torrent.isPaused()) {
            return UploadStatus.QUEUED;
        }

        if (status.isError()) {
            return UploadStatus.UNAVAILABLE_RANGE;
        }

        if (status.isPaused()) {
            return UploadStatus.PAUSED;
        } else {
            TorrentState state = status.getState();

            switch (state) {
            case DOWNLOADING:
            case FINISHED:
            case SEEDING:
                return UploadStatus.UPLOADING;
            case QUEUED_FOR_CHECKING:
            case CHECKING_FILES:
            case DOWNLOADING_METADATA:
            case ALLOCATING:
                return UploadStatus.CONNECTING;
            default:
                throw new UnsupportedOperationException("Unknown state: " + state);
            }
        }
    }

    @Override
    public UploadStatus getLastTransferState() {
        return getState();
    }

    @Override
    public boolean isBrowseHostEnabled() {
        return false;
    }

    @Override
    public int getGnutellaPort() {
        return 0;
    }

    @Override
    public String getUserAgent() {
        return BITTORRENT_UPLOAD;
    }

    @Override
    public int getQueuePosition() {
        return 0;
    }

    @Override
    public boolean isInactive() {

        if (torrent.getStatus().isPaused() || torrent.getStatus().isFinished()) {
            return true;
        }

        return false;
    }

    @Override
    public void measureBandwidth() {
        // uneeded using libtorrent rate
    }

    @Override
    public float getMeasuredBandwidth() throws InsufficientDataException {
        return (torrent.getUploadRate() / 1024);
    }

    @Override
    public float getAverageBandwidth() {
        // Unused
        return (torrent.getUploadRate() / 1024);
    }

    @Override
    public String getCustomIconDescriptor() {
        return BITTORRENT_UPLOAD;
    }

    @Override
    public UploadType getUploadType() {
        return UploadType.SHARED_FILE;
    }

    @Override
    public boolean isTLSCapable() {
        return false;
    }

    @Override
    public String getAddress() {
        return "torrent upload";
    }

    @Override
    public InetAddress getInetAddress() {
        return null;
    }

    @Override
    public int getPort() {
        return -1;
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        return null;
    }

    @Override
    public String getAddressDescription() {
        return null;
    }

    @Override
    public File getFile() {
        return torrent.getTorrentDataFile();
    }

    @Override
    public URN getUrn() {
        if (urn == null) {
            synchronized (this) {
                if (urn == null) {
                    try {
                        urn = URN.createSha1UrnFromHex(torrent.getSha1());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return urn;
    }

    @Override
    public int getNumUploadConnections() {
        return torrent.getNumConnections();
    }

    @Override
    public String getPresenceId() {
        return null;
    }

    @Override
    public float getSeedRatio() {
        return torrent.getSeedRatio();
    }
    
    @Override
    public void pause() {
        torrent.pause();
    }
    
    @Override
    public void resume() {
        torrent.resume();
    }
}
