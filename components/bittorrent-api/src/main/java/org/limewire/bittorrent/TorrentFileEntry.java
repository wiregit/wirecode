package org.limewire.bittorrent;

/**
 * Represents a file in the torrent. 
 */
public interface TorrentFileEntry {
    /**
     * Returns the index of the file in the torrent.
     */
    public int getIndex();

    /**
     * Returns the path of the file in the torrent.
     */
    public String getPath();
}
