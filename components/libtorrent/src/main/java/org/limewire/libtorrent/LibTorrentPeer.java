package org.limewire.libtorrent;

import com.sun.jna.Structure;

public class LibTorrentPeer extends Structure implements Structure.ByReference {
    public String peer_id;
    public String ip;
    public short source;
    public float up_speed;
    public float down_speed;
    public float payload_up_speed;
    public float payload_down_speed;
    public float progress;
    public String country;
}
