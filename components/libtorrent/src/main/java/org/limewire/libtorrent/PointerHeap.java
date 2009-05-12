package org.limewire.libtorrent;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

public class PointerHeap extends Memory {
    public PointerHeap() {
        super(Pointer.SIZE);
    }
}
