/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.IntegerType;
import com.sun.jna.Pointer;

public class LONG_PTR extends IntegerType {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1663821750397952382L;

    public LONG_PTR() {
        this(0);
    }

    public LONG_PTR(long value) {
        super(Pointer.SIZE, value);
    }
}