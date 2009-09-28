package org.limewire.libtorrent;

import com.sun.jna.Structure;

public class LibTorrentInfo extends Structure {
    public String sha1;
    public String created_by;
    public String comment;
}
