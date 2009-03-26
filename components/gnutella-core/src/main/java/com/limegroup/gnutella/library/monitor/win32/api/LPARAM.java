/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

public class LPARAM extends LONG_PTR {
    /**
	 * 
	 */
    private static final long serialVersionUID = -9217076851272676913L;

    public LPARAM() {
        this(0);
    }

    public LPARAM(long value) {
        super(value);
    }
}