/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class OVERLAPPED extends Structure {
    public int Internal;
    public int InternalHigh;
    public int Offset;
    public int OffsetHigh;
    public Pointer hEvent;
}