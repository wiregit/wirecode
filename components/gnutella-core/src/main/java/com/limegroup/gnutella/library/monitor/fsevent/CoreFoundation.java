package com.limegroup.gnutella.library.monitor.fsevent;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface CoreFoundation extends Library {

    public String strerror(int errnum);

    public String getLastError();

    Pointer CFArrayCreate(Pointer allocator, Pointer[] values, int numValues,
            CFArrayCallBacks callBacks);

    Pointer CFStringCreateWithCString(Pointer allocator, String string, int encoding);

    public Pointer CFStringCreateWithCString(String string);

    public Pointer CFRunLoopGetCurrent();

    public void CFRunLoopRun();

    public Pointer CFRetain(Pointer pointer);

    public Pointer CFArrayCreate(Pointer[] values);

    public Pointer CFArrayCreate(String[] stringVals);

}
