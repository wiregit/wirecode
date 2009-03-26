/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.IntegerType;

public class WORD extends IntegerType {
    public WORD() {
        this(0);
    }

    public WORD(long value) {
        super(2, value);
    }
}