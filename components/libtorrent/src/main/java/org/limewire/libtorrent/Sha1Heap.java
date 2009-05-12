package org.limewire.libtorrent;

import com.sun.jna.Memory;

public class Sha1Heap extends Memory {
    public Sha1Heap() {
        super(41);
    }
}
