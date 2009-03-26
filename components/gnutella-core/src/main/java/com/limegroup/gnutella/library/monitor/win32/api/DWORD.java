/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.IntegerType;

public class DWORD extends IntegerType {
    public DWORD() {
        this(0);
    }

    public DWORD(long value) {
        super(4, value);
    }
}