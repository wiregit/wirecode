/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

public class LRESULT extends LONG_PTR {
    /**
	 * 
	 */
    private static final long serialVersionUID = 4538316070138899521L;

    public LRESULT() {
        this(0);
    }

    public LRESULT(long value) {
        super(value);
    }
}