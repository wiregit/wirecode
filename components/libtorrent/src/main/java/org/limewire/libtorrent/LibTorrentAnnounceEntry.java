package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentTracker;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class LibTorrentAnnounceEntry extends Structure implements TorrentTracker {

    public String url;
    public int tier;

    public LibTorrentAnnounceEntry() {
        super();
    }

    public LibTorrentAnnounceEntry(Pointer p) {
        super(p);
    }

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    public String getURL() {
        return url;
    }
}
