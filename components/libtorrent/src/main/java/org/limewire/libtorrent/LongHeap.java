package org.limewire.libtorrent;

import com.sun.jna.Memory;

public class LongHeap extends Memory {
    public LongHeap() {
        super(20);
    }
}
