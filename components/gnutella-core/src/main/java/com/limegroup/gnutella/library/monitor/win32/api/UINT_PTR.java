/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.IntegerType;
import com.sun.jna.Pointer;

public class UINT_PTR extends IntegerType {
    /**
	 * 
	 */
    private static final long serialVersionUID = 5519980804346946264L;

    public UINT_PTR() {
        super(Pointer.SIZE);
    }

    public UINT_PTR(long value) {
        super(Pointer.SIZE, value);
    }

    public Pointer toPointer() {
        return Pointer.createConstant(longValue());
    }
}