package org.limewire.libtorrent;

import com.sun.jna.Structure;

public class LibTorrentAlert extends Structure {
    public String sha1;

    public String message;

    public int category;
}
