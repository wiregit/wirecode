/**
 * 
 */
package com.limegroup.gnutella.library.monitor.kqueue;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class kevent extends Structure implements Structure.ByReference {
    public int ident = -1; // identifier for this event

    public short filter; // filter for event

    public short flags; // general flags

    public int fflags; // filter-specific flags

    public int data; // filter-specific data

    public Pointer udata; // opaque user data identifier

    public final void set(kevent src) {
        data = src.data;
        fflags = src.fflags;
        filter = src.filter;
        flags = src.flags;
        ident = src.ident;
        udata = src.udata;
    }
}