/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.IntegerType;
import com.sun.jna.Pointer;

public class ULONG_PTR extends IntegerType {
    /**
	 * 
	 */
    private static final long serialVersionUID = 5318931073092596577L;

    public ULONG_PTR() {
        this(0);
    }

    public ULONG_PTR(long value) {
        super(Pointer.SIZE, value);
    }
}