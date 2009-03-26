package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;

/** LPHANDLE */
public class HANDLEByReference extends ByReference {
    public HANDLEByReference() {
        this(null);
    }

    public HANDLEByReference(HANDLE h) {
        super(Pointer.SIZE);
        setValue(h);
    }

    public void setValue(HANDLE h) {
        getPointer().setPointer(0, h != null ? h.getPointer() : null);
    }

    public HANDLE getValue() {
        Pointer p = getPointer().getPointer(0);
        if (p == null) {
            return null;
        }
        if (INVALID_HANDLE_VALUE.pointer.equals(p)) {
            return new INVALID_HANDLE_VALUE();
        }
        HANDLE h = new HANDLE();
        h.setPointer(p);
        return h;
    }
}