/**
 * 
 */
package com.limegroup.gnutella.library.monitor.kqueue;

import com.sun.jna.Structure;

public class timespec extends Structure {
    // / seconds
    public int tv_sec;

    // / nanoseconds
    public int tv_nsec;

    public timespec() {
        super();
    }

    // / Convenient constructor
    public timespec(int tv_sec, int tv_nsec) {
        super();
        this.tv_sec = tv_sec;
        this.tv_nsec = tv_nsec;
        write();
    }
}