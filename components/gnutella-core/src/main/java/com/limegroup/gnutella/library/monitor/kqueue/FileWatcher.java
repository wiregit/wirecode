/**
 * 
 */
package com.limegroup.gnutella.library.monitor.kqueue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.sun.jna.Pointer;

class FileWatcher extends Thread {
    /**
     * 
     */
    private final NaiveKQueueFileMonitor naiveKQueueFileMonitor;

    File file;

    int kq;

    kevent fileEvent = new kevent(), resultEvent = new kevent();

    public FileWatcher(NaiveKQueueFileMonitor naiveKQueueFileMonitor, File file, int mask)
            throws IOException {
        this.naiveKQueueFileMonitor = naiveKQueueFileMonitor;
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
                    new RuntimeException("kevent call returned negative value !").printStackTrace();
                    throw new RuntimeException("kevent call returned negative value !");
                } else if (nev > 0) {
                    if (KQueueEventMask.NOTE_DELETE.isSet(resultEvent.fflags)) {
                        // TODO broadcast this asynchronously
                        this.naiveKQueueFileMonitor.listeners.broadcast(new KQueueEvent(
                                KQueueEventType.DELETE, file.getPath()));
                        break;
                    }
                    if (KQueueEventMask.NOTE_RENAME.isSet(resultEvent.fflags)) {
                        // TODO broadcast this asynchronously
                        this.naiveKQueueFileMonitor.listeners.broadcast(new KQueueEvent(
                                KQueueEventType.RENAME, file.getPath()));
                    }
                    if (KQueueEventMask.NOTE_EXTEND.isSet(resultEvent.fflags)
                            || KQueueEventMask.NOTE_WRITE.isSet(resultEvent.fflags)) {
                        // TODO broadcast this asynchronously
                        this.naiveKQueueFileMonitor.listeners.broadcast(new KQueueEvent(
                                KQueueEventType.FILE_SIZE_CHANGED, file.getPath()));
                    }
                    if (KQueueEventMask.NOTE_ATTRIB.isSet(resultEvent.fflags)) {
                        // TODO broadcast this asynchronously
                        this.naiveKQueueFileMonitor.listeners.broadcast(new KQueueEvent(
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