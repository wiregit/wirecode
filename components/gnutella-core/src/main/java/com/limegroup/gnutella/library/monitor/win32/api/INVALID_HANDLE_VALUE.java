package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.Pointer;

public class INVALID_HANDLE_VALUE extends HANDLE {
    public static final INVALID_HANDLE_VALUE INVALID_HANDLE = new INVALID_HANDLE_VALUE();

    public INVALID_HANDLE_VALUE() {
        super.setPointer(Pointer.createConstant(-1));
    }

    public void setPointer(Pointer p) {
        throw new UnsupportedOperationException("Immutable reference");
    }
}
