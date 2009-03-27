package com.limegroup.gnutella.library.monitor.kqueue;

/* Copyright (c) 2008 Olivier Chafik, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */

import org.xbill.DNS.PTRRecord;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * CLibrary for BSD systems, including Mac OS X and FreeBSD
 * 
 * @author Olivier Chafik
 */
public interface CLibrary extends Library {
    
    

    String strerror(int errnum);
    
    // public CLibrary INSTANCE =
    // (CLibrary)Native.loadLibrary("/usr/lib/libc.dylib", CLibrary.class);
    public CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);

    public static final int O_RDONLY = 0x0000, // open for reading only
            O_WRONLY = 0x0001, // open for writing only
            O_RDWR = 0x0002, // open for reading and writing
            O_EVTONLY = 0x8000, // descriptor requested for event notifications 
                                // only

            // actions
            EV_ADD = 0x0001, // add event to kq (implies enable)
            EV_DELETE = 0x0002, // delete event from kq
            EV_ENABLE = 0x0004, // enable event
            EV_DISABLE = 0x0008, // disable event (not reported)

            // flags
            EV_ONESHOT = 0x0010, // only report one occurrence
            EV_CLEAR = 0x0020, // clear event state after reporting

            EV_SYSFLAGS = 0xF000, // reserved by system
            EV_FLAG0 = 0x1000, // filter-specific flag
            EV_FLAG1 = 0x2000, // filter-specific flag

            // returned values
            EV_EOF = 0x8000, // EOF detected
            EV_ERROR = 0x4000, // error, data contains errno

            EVFILT_VNODE = -4;

    // data/hint fflags for EVFILT_VNODE, shared with userspace

    /*
     * Error constants : ENOMEM, // The kernel failed to allocate enough memory
     * for the kernel queue. EMFILE, // The per-process descriptor table is
     * full. ENFILE, // The system file table is full. EACCES, // The process
     * does not have permission to register a filter. EFAULT = , // There was an
     * error reading or writing the kevent structure. EBADF = , // The specified
     * descriptor is invalid. EINTR = , // A signal was delivered before the
     * timeout expired and before any events were placed on the kqueue for
     * return. EINVAL = , // The specified time limit or filter is invalid.
     * ENOENT, // The event could not be found to be modified or deleted.
     * ENOMEM, // No memory was available to register the event. ESRCH, // The
     * specified process to attach to does not exist.
     */

    // public Pointer errno = null;
    public int perror(String s);

    // / just here to test that JNA works well...
    public int atol(String s);

    /**
     * TODO use kevent[]
     * 
     * @see https://jna.dev.java.net/javadoc/overview-summary.html
     * @param kq
     * @param events1 pointer to kevent[] array of things to monitor for
     *        changes
     * @param nchanges size of changelist
     * @param pointer2 pointer to kevent[] array of monitoring results
     * @param nevents size of relevant values returned in eventlist
     * @param timeout
     * @return
     */
    public int kevent(int kq, Pointer ptr, int nchanges, Pointer ptr1, int nevents,
            Pointer timeout);

    public int kqueue();

    public int open(String s, int i, int mode);

    public int close(int i);

    // / http://linux.about.com/library/cmd/blcmdl2_getdents.htm
    // / http://www.ipnom.com/FreeBSD-Man-Pages/getdents.2.html
    public int getdents(int fd, Pointer dirp, int count);

    public int open(String path, int evtonly);

}
