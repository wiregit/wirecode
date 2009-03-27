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

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

import com.limegroup.gnutella.library.monitor.kqueue.CLibrary.kevent;
import com.limegroup.gnutella.library.monitor.kqueue.CLibrary.timespec;
import com.sun.jna.Pointer;

/**
 * Looks like the java example was initially copied from:
 * 
 * http://julipedia.blogspot.com/2004/10/example-of-kqueue.html
 * 
 * 
 * Naive KQueue implementation of FileMonitor, for BSD-derived systems
 * (including Mac OS X).<br/> This implementation is naive in that it creates
 * one thread and one kqueue per watched file, which is heavy, slow and not
 * scalable.<br/> Does not handle FileEvent.FILE_CREATED event (only existing
 * files can be watched).<br/> Recursive watching is not implemented either.
 * 
 * @author Olivier Chafik
 */
public class NaiveKQueueFileMonitor {
    /**
     * kqueue man pages
     * http://people.freebsd.org/~jmg/kqueue.historic.man.html
     */
    private final Map<File, FileWatcher> fileWatchers;

    private final EventListenerList<KQueueEvent> listeners;

    public NaiveKQueueFileMonitor() {
        fileWatchers = new HashMap<File, FileWatcher>();
        listeners = new EventListenerList<KQueueEvent>();
    }

    public void addListener(EventListener<KQueueEvent> listener) {
        listeners.addListener(listener);
    }
    
    public boolean removeListener(EventListener<KQueueEvent> listener) {
        return listeners.removeListener(listener);
    }
    
    class FileWatcher extends Thread {
        File file;

        int kq;

        CLibrary.kevent fileEvent = new kevent(), resultEvent = new kevent();

        public FileWatcher(File file, int mask) throws IOException {
            this.file = file;
            kq = CLibrary.INSTANCE.kqueue();
            if (kq == -1)
                throw new IOException("Unable to create kqueue !");

            fileEvent.ident = CLibrary.INSTANCE.open(file.toString(), CLibrary.O_EVTONLY, 0);
            if (fileEvent.ident < 0)
                throw new FileNotFoundException(file.toString());

            fileEvent.filter = CLibrary.EVFILT_VNODE;
            fileEvent.flags = CLibrary.EV_ADD;// | CLibrary.EV_ONESHOT;
            fileEvent.fflags = mask;
            fileEvent.data = 0;
            fileEvent.udata = Pointer.NULL;
            fileEvent.write();
        }

        public void run() {
            try {
                Pointer pEvent = resultEvent.getPointer();

                // Set timeout to 1 second, so as to be interruptable quickly
                // enough without too much of a performance hit
                timespec timeout = new timespec(1, 0);
                Pointer pTimeout = timeout.getPointer();

                int nev = CLibrary.INSTANCE.kevent(kq, fileEvent.getPointer(), 1, Pointer.NULL, 0,
                        Pointer.NULL);
                if (nev != 0) {
                    new IOException("Failed to watch " + file).printStackTrace();
                    return;
                }

                for (;;) {
                    nev = CLibrary.INSTANCE.kevent(kq, Pointer.NULL, 0, pEvent, 1, pTimeout);
                    if (Thread.interrupted())
                        break;

                    resultEvent.read();
                    System.out.print(nev);
                    if (nev < 0) {
                        new RuntimeException("kevent call returned negative value !")
                                .printStackTrace();
                        throw new RuntimeException("kevent call returned negative value !");
                    } else if (nev > 0) {
                        if (KQueueEventMask.NOTE_DELETE.isSet(resultEvent.fflags)) {
                            // TODO broadcast this asynchronously
                            listeners.broadcast(new KQueueEvent(KQueueEventType.DELETE, file
                                    .getPath()));
                            break;
                        }
                        if (KQueueEventMask.NOTE_RENAME.isSet(resultEvent.fflags)) {
                            // TODO broadcast this asynchronously
                            listeners.broadcast(new KQueueEvent(KQueueEventType.RENAME, file
                                    .getPath()));
                        }
                        if (KQueueEventMask.NOTE_EXTEND.isSet(resultEvent.fflags)
                                || KQueueEventMask.NOTE_WRITE.isSet(resultEvent.fflags)) {
                            // TODO broadcast this asynchronously
                            listeners.broadcast(new KQueueEvent(KQueueEventType.FILE_SIZE_CHANGED,
                                    file.getPath()));
                        }
                        if (KQueueEventMask.NOTE_ATTRIB.isSet(resultEvent.fflags)) {
                            // TODO broadcast this asynchronously
                            listeners.broadcast(new KQueueEvent(
                                    KQueueEventType.FILE_ATTRIBUTES_CHANGED, file.getPath()));
                        }
                    }
                }
            } finally {
                // Close file handle
                CLibrary.INSTANCE.close(fileEvent.ident);
            }
        }
    }

    public synchronized void removeWatch(File file) {
        FileWatcher fw = fileWatchers.get(file);
        if (fw != null) {
            fileWatchers.remove(file);
            fw.interrupt();
        }
    }


    public synchronized void addWatch(File file) throws IOException {
        addWatch(file, KQueueEventMask.ALL_EVENTS.getMask());
    }
    
    public synchronized void addWatch(File file, int mask) throws IOException {
        FileWatcher fw = new FileWatcher(file, mask);
        fileWatchers.put(file, fw);
        fw.start();
    }
}