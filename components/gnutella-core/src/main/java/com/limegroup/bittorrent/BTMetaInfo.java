package com.limegroup.bittorrent;

import java.net.URI;
import java.security.MessageDigest;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMemento;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;

/**
 * Defines an interface for a class parsing information in a .torrent file.
 */
public interface BTMetaInfo {

    /**
     * @return piece length for this torrent
     */
    public abstract int getPieceLength();

    public abstract TorrentFileSystem getFileSystem();

    public abstract BTDiskManagerMemento getDiskManagerData();

    public abstract boolean isPrivate();

    public abstract void setContext(TorrentContext context);

    public long getAmountUploaded();

    public void countUploaded(int uploaded);

    public float getRatio();

    /**
     * Verifies whether the given hash matches the expect hash of a piece
     * 
     * @param sha1 the hash that was computed
     * @param pieceNum the piece for which the hash was computed
     * @return true if they match.
     */
    public abstract boolean verify(byte[] sha1, int pieceNum);

    /**
     * @return info hash
     */
    public abstract byte[] getInfoHash();

    /**
     * @return infohash URN
     */
    public abstract URN getURN();

    /**
     * @return FileDesc for the GUI.
     */
    public abstract FileDesc getFileDesc();

    public abstract void resetFileDesc();

    /**
     * @return number of pieces in this torrent
     */
    public abstract int getNumBlocks();

    public abstract String getName();

    /**
     * @return array of <tt>URL</tt> storing the addresses of the trackers
     */
    public abstract URI[] getTrackers();

    /**
     * Returns which message digest was used to create _hashes.
     * 
     * @return new Instance of the message digest that was used
     * 
     */
    public abstract MessageDigest getMessageDigest();

    /**
     * Serializes this, including information about the written ranges.
     */
    public abstract BTMetaInfoMemento toMemento();

}