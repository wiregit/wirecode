package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.Pointer;

public class HWND_BROADCAST extends HWND {
    public static final HWND_BROADCAST HWND_BROADCAST = new HWND_BROADCAST();

    public HWND_BROADCAST() {
        super.setPointer(Pointer.createConstant(0xFFFF));
    }

    public void setPointer(Pointer p) {
        throw new UnsupportedOperationException("Immutable reference");
    }
}
