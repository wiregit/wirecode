package com.limegroup.bittorrent;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;

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

    private final org.limewire.libtorrent.Torrent torrent;

    public BTUploader(org.limewire.libtorrent.Torrent torrent) {
        this.torrent = torrent;
    }

    public void stop() {

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
        // TODO update this method
        return UploadStatus.UPLOADING;
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
        //        
        // switch (_torrent.getState()) {
        // case PAUSED:
        // case STOPPED:
        // return true;
        // }
        // return false;

        // TODO implement
        return false;

    }

    public void measureBandwidth() {
        // uneeded using libtorrent rate
    }

    public float getMeasuredBandwidth() throws InsufficientDataException {
        // TODO return lbitorrent rate
        return 0;
    }

    public float getAverageBandwidth() {
        // Unused
        return -1;
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

    @Override
    public URN getUrn() {
        // torrent.getSha1()

        // TODO return urn
        return null;
    }

    @Override
    public int getNumUploadConnections() {
        // TODO get number for libtorrent
        return 0;
    }
}
