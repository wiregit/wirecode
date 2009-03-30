package com.limegroup.gnutella.library.monitor.kqueue;

/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
 * Parts Copyright (c) 2008 Olivier Chafik 
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
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Provides notification of file system changes. Actual capabilities may vary
 * slightly by platform.
 * <p>
 * Watched files which are removed from the filesystem are no longer watched.
 * 
 * @author twall
 */

public abstract class FileMonitor {

    public static final int FILE_CREATED = 0x1;

    public static final int FILE_DELETED = 0x2;

    public static final int FILE_MODIFIED = 0x4;

    public static final int FILE_ACCESSED = 0x8;

    public static final int FILE_NAME_CHANGED_OLD = 0x10;

    public static final int FILE_NAME_CHANGED_NEW = 0x20;

    public static final int FILE_RENAMED = FILE_NAME_CHANGED_OLD | FILE_NAME_CHANGED_NEW;

    public static final int FILE_SIZE_CHANGED = 0x40;

    public static final int FILE_ATTRIBUTES_CHANGED = 0x80;

    public static final int FILE_SECURITY_CHANGED = 0x100;

    // public static final int FILE_WATCHED = 0x200;
    // public static final int FILE_UNWATCHED = 0x400;

    public static final int FILE_ANY = 0x1FF;

    public interface FileListener extends EventListener {
        public void fileChanged(FileEvent e);
    }

    public class FileEvent extends EventObject {
        private final File file;

        private final int type;

        public FileEvent(File file, int type) {
            super(FileMonitor.this);
            this.file = file;
            this.type = type;
        }

        public File getFile() {
            return file;
        }

        public int getType() {
            return type;
        }

        // / this is just to ease up events debugging
        /*
         * public String toString() { Integer typeObj = new Integer(type); for
         * (Field field : FileMonitor.class.getDeclaredFields()) { try { if
         * (Modifier.isStatic(field.getModifiers()) &&
         * typeObj.equals(field.get(null))) return
         * field.getName().replace("FILE_", "") + ": " + file; } catch
         * (Exception e) { e.printStackTrace(); } } return "FileEvent: " + file
         * + ": " + type; }
         */
    }

    private final Map watched = new HashMap();

    protected List listeners = new ArrayList();

    protected abstract void watch(File file, int mask, boolean recursive) throws IOException;

    protected abstract void unwatch(File file);

    protected abstract void dispose();

    public void addWatch(File dir) throws IOException {
        addWatch(dir, FILE_ANY);
    }

    public void addWatch(File dir, int mask) throws IOException {
        addWatch(dir, mask, dir.isDirectory());
    }

    public void addWatch(File dir, int mask, boolean recursive) throws IOException {
        watched.put(dir, new Integer(mask));
        watch(dir, mask, recursive);
    }

    public void removeWatch(File file) {
        if (watched.remove(file) != null) {
            unwatch(file);
        }
    }

    protected void notify(FileEvent e) {
        for (int i = 0, len = listeners.size(); i < len; i++) {
            ((FileListener) listeners.get(i)).fileChanged(e);
        }
    }

    public synchronized void addFileListener(FileListener x) {
        listeners.add(x);
    }

    public synchronized void removeFileListener(FileListener x) {
        listeners.remove(x);
    }

    protected void finalize() {
        for (Iterator i = watched.keySet().iterator(); i.hasNext();) {
            removeWatch((File) i.next());
        }
        dispose();
    }

    /** Create a FileMonitor instance */
    static FileMonitor createInstance() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Mac") || os.indexOf("BSD") >= 0) {
            try {
                // return new NaiveKQueueFileMonitor();
                return new KQueueFileMonitor();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new Error("FileMonitor not implemented for " + os);
        }
    }

    /** Shared FileMonitor instance */
    private static FileMonitor sharedInstance;

    /** Get the shared FileMonitor instance */
    public static FileMonitor getInstance() {
        if (sharedInstance == null) {
            sharedInstance = createInstance();
        }
        return sharedInstance;
    }

    /**
     * KQueue implementation of FileMonitor, for BSD-derived systems (including
     * Mac OS X).<br/> Only one dedicated thread and one kqueue is created by
     * this monitor, designed to be as scalable as possible.<br/> TODO handle
     * the FileEvent.FILE_CREATED event and recursive flag (right now, only
     * existing files can be watched).<br/>
     * 
     * @author Olivier Chafik
     */
    private static class KQueueFileMonitor extends FileMonitor implements Runnable {
        // / map from file descriptor to file
        private final Map/* <Integer, File> */fdToFile = new TreeMap();

        // / map from file to file descriptor
        private final Map/* <File, Integer> */fileToFd = new TreeMap();

        int notificationFileDescriptor;

        File notificationFile;

        final int kqueueHandle;

        timespec timeout;

        // / Map from file to pending declaration kevent
        private final Map/* <File, kevent> */pendingDeclarationEvents = new LinkedHashMap();

        public KQueueFileMonitor() throws IOException {
            try {
                notificationFile = File.createTempFile("kqueueFileWatch", ".sync");
                notificationFile.createNewFile();
                notificationFile.deleteOnExit();

                add(notificationFile, FileMonitor.FILE_ATTRIBUTES_CHANGED, false);
                notificationFileDescriptor = ((Integer) fileToFd.get(notificationFile)).intValue();

                // as there is a synchronization file, we will be able to
                // interrupt calls to kevent easily. So we setup a timeout as
                // high as we want :
                timeout = new timespec(100, 0);
            } catch (IOException e) {
                System.err.println("Failed to create notification file " + notificationFile);

                // we'll do fine even without notification file : however, there
                // will be some delay to add / remove files
                timeout = new timespec(0, 500000000);
            }
            kqueueHandle = CLibrary.INSTANCE.kqueue();
            if (kqueueHandle == -1)
                throw new IOException("Unable to create kqueue !");
        }

        protected int convertMask(int fileMonitorMask) {
            int keventMask = 0;
            if ((fileMonitorMask & FileMonitor.FILE_DELETED) != 0) {
                keventMask |= CLibrary.NOTE_DELETE;
            }
            if ((fileMonitorMask & FileMonitor.FILE_MODIFIED) != 0) {
                keventMask |= CLibrary.NOTE_WRITE;
            }
            if ((fileMonitorMask & FileMonitor.FILE_RENAMED) != 0) {
                keventMask |= CLibrary.NOTE_RENAME;
            }
            if ((fileMonitorMask & FileMonitor.FILE_SIZE_CHANGED) != 0) {
                keventMask |= CLibrary.NOTE_EXTEND;
            }
            if ((fileMonitorMask & FileMonitor.FILE_ACCESSED) != 0) {
                keventMask |= CLibrary.FILE_NOTIFY_CHANGE_LAST_ACCESS;
            }
            if ((fileMonitorMask & FileMonitor.FILE_ATTRIBUTES_CHANGED) != 0) {
                keventMask |= CLibrary.NOTE_ATTRIB;
            }
            if ((fileMonitorMask & FileMonitor.FILE_SECURITY_CHANGED) != 0) {
                keventMask |= CLibrary.NOTE_ATTRIB;
            }
            return keventMask;
        }

        // final Pointer NATIVE_0L = Pointer.createConstant(0), NATIVE_1L =
        // Pointer.createConstant(1);

        protected synchronized void add(File file, int keventMask, boolean recursive)
                throws IOException {
            int fd;
            Integer fdObj = (Integer) fileToFd.get(file);
            if (fdObj == null) {
                fd = CLibrary.INSTANCE.open(file.toString(), CLibrary.O_EVTONLY, 0);
                if (fd < 0)
                    throw new FileNotFoundException(file.toString());

                fdObj = new Integer(fd);
                fdToFile.put(fdObj, file);
                fileToFd.put(file, fdObj);
            } else {
                fd = fdObj.intValue();
            }

            kevent ke = new kevent();
            ke.ident = fd;
            ke.filter = CLibrary.EVFILT_VNODE;
            ke.flags = CLibrary.EV_ADD | CLibrary.EV_CLEAR;
            ke.fflags = keventMask;
            ke.data = 0;

            recursive = recursive && file.isDirectory();
            // ke.udata = recursive ? NATIVE_1L : NATIVE_0L;
            ke.udata = null;

            synchronized (pendingDeclarationEvents) {
                pendingDeclarationEvents.put(file, ke);
            }

            // System.err.println("File " + file + " : recursive = "+recursive);
            if (recursive) {
                File[] children = file.listFiles();
                for (int i = children.length; i-- != 0;) {
                    File child = children[i];
                    System.err.println("Child " + child);
                    add(child, keventMask, recursive);
                }
            }
        }

        protected synchronized void remove(File file) {
            Integer fdObj = (Integer) fileToFd.remove(file);
            if (fdObj == null)
                return;

            fdToFile.remove(fdObj);

            // This will automatically remove the file from the kqueue :
            CLibrary.INSTANCE.close(fdObj.intValue());
        }

        public synchronized void dispose() {
            if (loopThread != null)
                loopThread.interrupt();
        }

        protected void finalize() {
            notificationFile.delete();
            if (kqueueHandle != -1) {
                CLibrary.INSTANCE.close(kqueueHandle);
            }
        }

        /**
         * @param f
         * @param mask
         * @param recurse ignored !
         * @throws IOException
         */
        public synchronized void watch(File f, int fileMonitorMask, boolean recurse)
                throws IOException {
            doWatch(f, convertMask(fileMonitorMask), recurse);
        }

        protected void doWatch(File f, int keventMask, boolean recurse) throws IOException {
            checkStarted();
            add(f, keventMask, recurse);
            tryAndSync();
        }

        // / Whether the file monitor thread was started or not
        protected boolean started;

        protected Thread loopThread;

        protected synchronized void checkStarted() {
            if (loopThread == null)
                (loopThread = new Thread(this)).start();
        }

        public synchronized void unwatch(File file) {
            remove(file);
            tryAndSync();
        }

        /**
         * This will try to cause the kevent call in run() to return before the
         * timeout, by modifying the notificationFile
         */
        protected void tryAndSync() {
            notificationFile.setLastModified(System.currentTimeMillis());
        }

        public void run() {
            kevent event = new kevent();
            Pointer pEvent = event.getPointer();
            timeout.write();
            Pointer pTimeout = timeout.getPointer();

            kevent modifEvent = new kevent();
            modifEvent.write();
            Pointer pModifEvent = modifEvent.getPointer();

            for (; !Thread.interrupted();) {
                // First, handle pending declarations : add watched files
                synchronized (pendingDeclarationEvents) {
                    for (Iterator entrit = pendingDeclarationEvents.entrySet().iterator(); entrit
                            .hasNext();) {
                        Map.Entry/* <File, kevent> */entry = (Map.Entry) entrit.next();
                        modifEvent.set((kevent) entry.getValue());
                        modifEvent.write();

                        kevent rEvent = new kevent();
                        rEvent.write();
                        int nev = CLibrary.INSTANCE.kevent(kqueueHandle, pModifEvent, 1,
                                rEvent.getPointer(), 1, null);
                        if (nev != 0)
                            new IOException("kevent did not like modification event for "
                                    + entry.getKey()+ " " + CLibrary.INSTANCE.strerror(Native.getLastError())).printStackTrace() ;
                    }
                    pendingDeclarationEvents.clear();
                }

                // Now listen to events : call returns on first event or after
                // timeout is reached.
                int nev = CLibrary.INSTANCE.kevent(kqueueHandle, Pointer.NULL, 0, pEvent, 1,
                        pTimeout);
                event.read();
                System.out.println("kevent = " + nev);
                if (nev < 0) {
                    //throw new RuntimeException("kevent call returned negative value !");
                } else if (nev > 0) {
                    if (notificationFileDescriptor == event.ident) {
                        // This is a fake event, triggered by modification of
                        // the synchronization file.
                        // It is only meant to interrupt the blocking call to
                        // kevent and handle pending declarations before doing
                        // another blocking call to kevent.
                        continue;
                    }
                    File file = (File) fdToFile.get(new Integer(event.ident));
                    if ((event.fflags & CLibrary.NOTE_DELETE) != 0) {
                        remove(file);
                        notify(new FileEvent(file, FileMonitor.FILE_DELETED));
                    }
                    if ((event.fflags & CLibrary.NOTE_RENAME) != 0) {
                        // A file that is renamed is also declared as deleted,
                        // and its track is lost.
                        remove(file);
                        notify(new FileMonitor.FileEvent(file, FileMonitor.FILE_RENAMED));
                        notify(new FileMonitor.FileEvent(file, FileMonitor.FILE_DELETED));
                    }
                    if ((event.fflags & CLibrary.NOTE_EXTEND) != 0
                            || (event.fflags & CLibrary.NOTE_WRITE) != 0) {
                        notify(new FileMonitor.FileEvent(file, FileMonitor.FILE_SIZE_CHANGED));
                    }
                    if ((event.fflags & CLibrary.NOTE_ATTRIB) != 0) {
                        // TODO handle recursivity : if file is a directory and
                        // is marked as recursively watched, add children
                        /*
                         * if (event.udata.getPointer().getInt(0) == 1) // Was
                         * this file watched recursively ? if
                         * (file.isDirectory()) for (File child :
                         * file.listFiles()) if (!fileToFd.containsKey(child)) {
                         * // child is not watched yet : add it notify(new
                         * FileEvent(child, FileMonitor.FILE_CREATED)); try {
                         * doWatch(child, event.fflags, true); } catch
                         * (IOException e) {
                         * System.err.println("Failed to add recursive file "
                         * +child +" : "+e); } }
                         */
                        notify(new FileMonitor.FileEvent(file, FileMonitor.FILE_ATTRIBUTES_CHANGED));
                    }
                }
            }
        }
    }

    private static class INotifyFileMonitor extends FileMonitor {
        protected void watch(File file, int mask, boolean recursive) {

        }

        protected void unwatch(File file) {
        }

        protected void dispose() {
        }
    }
}
