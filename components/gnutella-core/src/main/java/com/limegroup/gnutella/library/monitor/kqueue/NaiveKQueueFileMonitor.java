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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.library.monitor.FileMonitor;
import com.limegroup.gnutella.library.monitor.kqueue.CLibrary.kevent;
import com.limegroup.gnutella.library.monitor.kqueue.CLibrary.timespec;
import com.limegroup.gnutella.library.monitor.win32.Kernel32;
import com.sun.jna.Pointer;

/**
 * Naive KQueue implementation of FileMonitor, for BSD-derived systems
 * (including Mac OS X).<br/> This implementation is naive in that it creates
 * one thread and one kqueue per watched file, which is heavy, slow and not
 * scalable.<br/> Does not handle FileEvent.FILE_CREATED event (only existing
 * files can be watched).<br/> Recursive watching is not implemented either.
 * 
 * @author Olivier Chafik
 */
class NaiveKQueueFileMonitor /*extends FileMonitor*/ {
//    private Map<File, FileWatcher> fileWatchers = new HashMap<File, FileWatcher>();
//
//    class FileWatcher extends Thread {
//        File file;
//
//        int kq;
//
//        CLibrary.kevent fileEvent = new kevent(), resultEvent = new kevent();
//
//        public FileWatcher(File file, int mask) throws IOException {
//            this.file = file;
//            kq = CLibrary.INSTANCE.kqueue();
//            if (kq == -1)
//                throw new IOException("Unable to create kqueue !");
//
//            fileEvent.ident = CLibrary.INSTANCE.open(file.toString(), CLibrary.O_EVTONLY, 0);
//            if (fileEvent.ident < 0)
//                throw new FileNotFoundException(file.toString());
//
//            fileEvent.filter = CLibrary.EVFILT_VNODE;
//            fileEvent.flags = CLibrary.EV_ADD;// | CLibrary.EV_ONESHOT;
//            fileEvent.fflags = mask;
//            fileEvent.data = 0;
//            fileEvent.udata = Pointer.NULL;
//            fileEvent.write();
//        }
//
//        public void run() {
//            try {
//                Pointer pEvent = resultEvent.getPointer();
//
//                // Set timeout to 1 second, so as to be interruptable quickly
//                // enough without too much of a performance hit
//                timespec timeout = new timespec(1, 0);
//                Pointer pTimeout = timeout.getPointer();
//
//                int nev = CLibrary.INSTANCE.kevent(kq, fileEvent.getPointer(), 1, Pointer.NULL, 0,
//                        Pointer.NULL);
//                if (nev != 0) {
//                    new IOException("Failed to watch " + file).printStackTrace();
//                    return;
//                }
//
//                for (;;) {
//                    nev = CLibrary.INSTANCE.kevent(kq, Pointer.NULL, 0, pEvent, 1, pTimeout);
//                    if (Thread.interrupted())
//                        break;
//
//                    resultEvent.read();
//                    System.out.print(nev);
//                    if (nev < 0) {
//                        new RuntimeException("kevent call returned negative value !")
//                                .printStackTrace();
//                        throw new RuntimeException("kevent call returned negative value !");
//                    } else if (nev > 0) {
//                        if ((resultEvent.fflags & CLibrary.NOTE_DELETE) != 0) {
//                            NaiveKQueueFileMonitor.this.notify(new FileEvent(file,
//                                    FileMonitor.FILE_DELETED));
//                            break;
//                        }
//                        if ((resultEvent.fflags & CLibrary.NOTE_RENAME) != 0) {
//                            NaiveKQueueFileMonitor.this.notify(new FileEvent(file,
//                                    FileMonitor.FILE_RENAMED));
//                        }
//                        if ((resultEvent.fflags & CLibrary.NOTE_EXTEND) != 0
//                                || (resultEvent.fflags & CLibrary.NOTE_WRITE) != 0) {
//                            NaiveKQueueFileMonitor.this.notify(new FileEvent(file,
//                                    FileMonitor.FILE_SIZE_CHANGED));
//                        }
//                        if ((resultEvent.fflags & CLibrary.NOTE_ATTRIB) != 0) {
//                            NaiveKQueueFileMonitor.this.notify(new FileMonitor.FileEvent(file,
//                                    FileMonitor.FILE_ATTRIBUTES_CHANGED));
//                        }
//                    }
//                }
//            } finally {
//                // Close file handle
//                CLibrary.INSTANCE.close(fileEvent.ident);
//            }
//        }
//    }
//
//    @Override
//    protected synchronized void dispose() {
//        for (FileWatcher fw : fileWatchers.values())
//            fw.interrupt();
//    }
//
//    @Override
//    protected synchronized void unwatch(File file) {
//        FileWatcher fw = fileWatchers.get(file);
//        if (fw != null) {
//            fileWatchers.remove(file);
//            fw.interrupt();
//        }
//    }
//
//    @Override
//    protected synchronized void watch(File file, int mask, boolean recursive) throws IOException {
//        FileWatcher fw = new FileWatcher(file, convertMask(mask));
//        fileWatchers.put(file, fw);
//        fw.start();
//    }
//
//    protected int convertMask(int mask) {
//        int result = 0;
//        if ((mask & FileMonitor.FILE_DELETED) != 0) {
//            result |= CLibrary.NOTE_DELETE;
//        }
//        if ((mask & FileMonitor.FILE_MODIFIED) != 0) {
//            result |= CLibrary.NOTE_WRITE;
//        }
//        if ((mask & FileMonitor.FILE_RENAMED) != 0) {
//            result |= CLibrary.NOTE_RENAME;
//        }
//        if ((mask & FileMonitor.FILE_SIZE_CHANGED) != 0) {
//            result |= CLibrary.NOTE_EXTEND;
//        }
//        if ((mask & FileMonitor.FILE_ACCESSED) != 0) {
//            result |= Kernel32.FILE_NOTIFY_CHANGE_LAST_ACCESS;
//        }
//        if ((mask & FileMonitor.FILE_ATTRIBUTES_CHANGED) != 0) {
//            result |= CLibrary.NOTE_ATTRIB;
//        }
//        if ((mask & FileMonitor.FILE_SECURITY_CHANGED) != 0) {
//            result |= CLibrary.NOTE_ATTRIB;
//        }
//        return result;
//    }
}