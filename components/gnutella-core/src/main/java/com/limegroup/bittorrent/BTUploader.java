package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.limewire.libtorrent.LibTorrentState;
import org.limewire.libtorrent.LibTorrentStatus;
import org.limewire.libtorrent.Torrent;
import org.limewire.libtorrent.TorrentEvent;
import org.limewire.libtorrent.TorrentSHA1ConversionUtils;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.uploader.UploadType;

/**
 * A facade for the GUI to treat a single BitTorrent download as a single
 * upload.
 */
public class BTUploader implements Uploader {

    private final ActivityCallback activityCallback;

    private final Torrent torrent;

    private volatile URN urn = null;

    private boolean cancelled = false;

    public BTUploader(Torrent torrent, ActivityCallback activityCallback) {
        this.torrent = torrent;
        this.activityCallback = activityCallback;
        torrent.addListener(new EventListener<TorrentEvent>() {
            public void handleEvent(TorrentEvent event) {
                if (event == TorrentEvent.STOPPED) {
                    // TODO cleanup
                    cancelled = true;
                    BTUploader.this.activityCallback.removeUpload(BTUploader.this);
                }
            };
        });
    }

    public void stop() {
        cancelled = activityCallback.promptTorrentUploadCancel(torrent);
        if (cancelled) {
            torrent.stop();
            activityCallback.removeUpload(this);
        }
    }

    public String getFileName() {
        return torrent.getName();
    }

    public long getFileSize() {
        return torrent.getTotalSize();
    }

    public FileDesc getFileDesc() {
        return null;
    }

    public int getIndex() {
        // negative will make sure it never conflicts with regular uploads
        return 0 - Math.abs(hashCode());
    }

    public long amountUploaded() {
        return torrent.getTotalUploaded();
    }

    public long getTotalAmountUploaded() {
        return torrent.getTotalUploaded();
    }

    public String getHost() {
        return BITTORRENT_UPLOAD;
    }

    public UploadStatus getState() {
        LibTorrentStatus status = torrent.getStatus();

        if (status == null) {
            return UploadStatus.CONNECTING;
        }

        if (cancelled) {
            return UploadStatus.CANCELLED;
        }

        if (status.isPaused()) {
            return UploadStatus.UPLOADING;
        } else {
            LibTorrentState state = LibTorrentState.forId(status.state);

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

    public UploadStatus getLastTransferState() {
        return UploadStatus.UPLOADING;
    }

    public boolean isBrowseHostEnabled() {
        return false;
    }

    public int getGnutellaPort() {
        return 0;
    }

    public String getUserAgent() {
        return BITTORRENT_UPLOAD;
    }

    public int getQueuePosition() {
        return 0;
    }

    public boolean isInactive() {

        if (torrent.getStatus().isPaused() || torrent.getStatus().isFinished()) {
            return true;
        }

        return false;
    }

    public void measureBandwidth() {
        // uneeded using libtorrent rate
    }

    public float getMeasuredBandwidth() throws InsufficientDataException {
        return (torrent.getUploadRate() / 1024);
    }

    public float getAverageBandwidth() {
        // Unused
        return (torrent.getUploadRate() / 1024);
    }

    public String getCustomIconDescriptor() {
        if (torrent.isSingleFileTorrent()) {
            return null;
        }
        return BITTORRENT_UPLOAD;
    }

    public UploadType getUploadType() {
        return UploadType.SHARED_FILE;
    }

    public boolean isTLSCapable() {
        return false;
    }

    public String getAddress() {
        return "torrent upload";
    }

    public InetAddress getInetAddress() {
        return null;
    }

    public int getPort() {
        return -1;
    }

    public InetSocketAddress getInetSocketAddress() {
        return null;
    }

    @Override
    public String getAddressDescription() {
        return null;
    }

    @Override
    public File getFile() {
        if (torrent.isFinished()) {
            return torrent.getCompleteFile();
        } else {
            return torrent.getIncompleteFile();
        }
    }

    public URN getUrn() {
        if (urn == null) {
            synchronized (this) {
                if (urn == null) {
                    try {
                        urn = URN.createSHA1UrnFromBytes(TorrentSHA1ConversionUtils
                                .fromHexString(torrent.getSha1()));
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
        return torrent.getNumUploads();
    }
}
