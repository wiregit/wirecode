package org.limewire.libtorrent;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class LibTorrentAnnounceEntry extends Structure {

    public String url;
    public int tier;
    
    public LibTorrentAnnounceEntry() {
        super();
    }
    
    public LibTorrentAnnounceEntry(Pointer p) {
        super(p);
    }
}
