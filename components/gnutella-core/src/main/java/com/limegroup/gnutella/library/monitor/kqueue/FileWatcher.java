/**
 * 
 */
package com.limegroup.gnutella.library.monitor.kqueue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.limewire.listener.EventListenerList;

import com.sun.jna.Pointer;

public class FileWatcher extends Thread {
    private final File file;

    private kevent fileEvent = null;

    private kevent resultEvent = null;

    private final EventListenerList<KQueueEvent> listeners;

    private final int mask;

    private int kq = -1;

    public FileWatcher(File file, int mask) throws IOException {
        this(new EventListenerList<KQueueEvent>(), file, mask);
    }

    public FileWatcher(EventListenerList<KQueueEvent> listeners, File file, int mask)
            throws IOException {
        this.file = file;
        this.listeners = listeners;
        this.mask = mask;
    }

    public void init() throws IOException {
        if (kq == -1) {
            kq = CLibrary.INSTANCE.kqueue();
            if (kq == -1) {
                throw new IOException("Unable to create kqueue !");
            }

            if (fileEvent == null) {
                fileEvent = new kevent();
                fileEvent.ident = CLibrary.INSTANCE.open(file.toString(), CLibrary.O_EVTONLY, 0);
                if (fileEvent.ident < 0) {
                    throw new FileNotFoundException(file.toString());
                }

                fileEvent.filter = CLibrary.EVFILT_VNODE;
                fileEvent.flags = CLibrary.EV_ADD;// | CLibrary.EV_ONESHOT;
                fileEvent.fflags = mask;
                fileEvent.data = 0;
                fileEvent.udata = Pointer.NULL;
                fileEvent.write();
            }

            if (resultEvent == null) {
                resultEvent = new kevent();
            }

            int nev = CLibrary.INSTANCE.kevent(kq, fileEvent.getPointer(), 1, Pointer.NULL, 0,
                    Pointer.NULL);
            if (nev != 0) {
                new IOException("Failed to watch " + file).printStackTrace();
                return;
            }
        }

    }

    public void run() {
        try {

            Pointer pEvent = resultEvent.getPointer();

            // Set timeout to 1 second, so as to be interruptable quickly
            // enough without too much of a performance hit
            timespec timeout = new timespec(1, 0);
            Pointer pTimeout = timeout.getPointer();

            for (;;) {
                int nev = CLibrary.INSTANCE.kevent(kq, Pointer.NULL, 0, pEvent, 1, pTimeout);
                if (Thread.interrupted())
                    break;

                resultEvent.read();
                System.out.print(nev);
                if (nev < 0) {
                    new RuntimeException("kevent call returned negative value !").printStackTrace();
                    throw new RuntimeException("kevent call returned negative value !");
                } else if (nev > 0) {
                    if (KQueueEventMask.NOTE_DELETE.isSet(resultEvent.fflags)) {
                        // TODO broadcast this asynchronously
                        listeners
                                .broadcast(new KQueueEvent(KQueueEventType.DELETE, file.getPath()));
                        break;
                    }
                    if (KQueueEventMask.NOTE_RENAME.isSet(resultEvent.fflags)) {
                        // TODO broadcast this asynchronously
                        listeners
                                .broadcast(new KQueueEvent(KQueueEventType.RENAME, file.getPath()));
                    }
                    if (KQueueEventMask.NOTE_EXTEND.isSet(resultEvent.fflags)
                            || KQueueEventMask.NOTE_WRITE.isSet(resultEvent.fflags)) {
                        // TODO broadcast this asynchronously
                        listeners.broadcast(new KQueueEvent(KQueueEventType.FILE_SIZE_CHANGED, file
                                .getPath()));
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