package com.limegroup.gnutella.downloader.serial;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

import com.limegroup.bittorrent.TorrentFileSystem;

/**
 * Defines an interface from which bittorrent meta-info can be saved and recreated over
 * different sessions.
 */
public interface BTMetaInfoMemento {

    List<byte[]> getHashes();

    void setHashes(List<byte[]> hashes);

    int getPieceLength();

    void setPieceLength(int pieceLength);

    TorrentFileSystem getFileSystem();

    void setFileSystem(TorrentFileSystem fileSystem);

    byte[] getInfoHash();

    void setInfoHash(byte[] infoHash);

    float getRatio();

    void setRatio(float ratio);

    Serializable getFolderData();

    void setFolderData(Serializable folderData);

    URI[] getTrackers();

    void setTrackers(URI[] trackers);

    boolean isPrivate();

    void setPrivate(boolean aPrivate);
}
