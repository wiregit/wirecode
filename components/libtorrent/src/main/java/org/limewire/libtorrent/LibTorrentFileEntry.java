package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentFileEntry;

import com.sun.jna.Structure;
import com.sun.jna.WString;

/**
 * Represents a file in the torrent. 
 */
public class LibTorrentFileEntry extends Structure implements Structure.ByReference, TorrentFileEntry {
    public int index;
    public WString path;

    @Override
    public int getIndex() {
       return index;
    }

    @Override
    public String getPath() {
        return path.toString();
    }
    
    @Override
    public String toString() {
        return index + " - " + path;
    }
}
