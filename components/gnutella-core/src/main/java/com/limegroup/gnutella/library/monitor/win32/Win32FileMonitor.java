package com.limegroup.gnutella.library.monitor.win32;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

import com.limegroup.gnutella.library.monitor.win32.api.FILE_NOTIFY_INFORMATION;
import com.limegroup.gnutella.library.monitor.win32.api.HANDLE;
import com.limegroup.gnutella.library.monitor.win32.api.HANDLEByReference;
import com.limegroup.gnutella.library.monitor.win32.api.INVALID_HANDLE_VALUE;
import com.limegroup.gnutella.library.monitor.win32.api.Kernel32;
import com.limegroup.gnutella.library.monitor.win32.api.Kernel32Utils;
import com.limegroup.gnutella.library.monitor.win32.api.OVERLAPPED;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class Win32FileMonitor {
    private HANDLE port;

    private Thread watcher;

    private final Map<File, Integer> watched = new HashMap<File, Integer>();

    private final Map<File, FileInfo> fileMap = new HashMap<File, FileInfo>();

    private final Map<HANDLE, FileInfo> handleMap = new HashMap<HANDLE, FileInfo>();

    private final EventListenerList<W32NotifyActionEvent> listeners = new EventListenerList<W32NotifyActionEvent>();

    public synchronized void init() throws IOException {
        if (port == null) {
            Kernel32 klib = Kernel32.INSTANCE;
            port = klib.CreateIoCompletionPort(INVALID_HANDLE_VALUE.INVALID_HANDLE, port, null, 0);
            if (port == null) {
                int err = klib.GetLastError();
                throw new IOException("Error initializing IOCompletionPort: '"
                        + Kernel32Utils.getSystemError(err) + "' (" + err + ")");
            } else {
                watcher = new EventPoller("W32 File Monitor");
                watcher.start();
            }
        }
    }

    public synchronized void addWatch(File dir) throws IOException {
        addWatch(dir, W32NotifyEventMask.ALL_EVENTS.getMask(), false);
    }

    public synchronized void addWatch(File dir, boolean recursive) throws IOException {
        addWatch(dir, W32NotifyEventMask.ALL_EVENTS.getMask(), recursive);
    }

    public synchronized void addWatch(File dir, int mask) throws IOException {
        addWatch(dir, mask, false);
    }

    public synchronized void addWatch(File dir, int mask, boolean recursive) throws IOException {
        watch(dir, mask, recursive);
    }

    public synchronized void removeWatch(File file) {
        watched.remove(file);
        FileInfo finfo = fileMap.remove(file);
        if (finfo != null) {
            handleMap.remove(finfo.handle);
            Kernel32 klib = Kernel32.INSTANCE;
            klib.CloseHandle(finfo.handle);
        }
    }

    protected synchronized void watch(File file, int eventMask, boolean recursive)
            throws IOException {
        if (port == null) {
            throw new IOException("Cannot add watches to the FileMonitor before it is initialized.");
        }
        watched.put(file, new Integer(eventMask));
        Kernel32 klib = Kernel32.INSTANCE;
        int mask = Kernel32.FILE_SHARE_READ | Kernel32.FILE_SHARE_WRITE
                | Kernel32.FILE_SHARE_DELETE;
        int flags = Kernel32.FILE_FLAG_BACKUP_SEMANTICS | Kernel32.FILE_FLAG_OVERLAPPED;
        HANDLE handle = klib.CreateFile(file.getAbsolutePath(), Kernel32.FILE_LIST_DIRECTORY, mask,
                null, Kernel32.OPEN_EXISTING, flags, null);
        if (INVALID_HANDLE_VALUE.INVALID_HANDLE.equals(handle)) {
            throw new IOException("Unable to open " + file + " (" + klib.GetLastError() + ")");
        }
        FileInfo finfo = new FileInfo(file, handle, eventMask, recursive);
        fileMap.put(file, finfo);
        handleMap.put(handle, finfo);
        // Existing port is returned
        port = klib.CreateIoCompletionPort(handle, port, handle.getPointer(), 0);
        if (INVALID_HANDLE_VALUE.INVALID_HANDLE.equals(port)) {
            throw new IOException("Unable to create/use I/O Completion port " + "for " + file
                    + " (" + klib.GetLastError() + ")");
        }

        if (!klib.ReadDirectoryChangesW(handle, finfo.info, finfo.info.size(), recursive,
                eventMask, finfo.infoLength, finfo.overlapped, null)) {
            int err = klib.GetLastError();
            throw new IOException("ReadDirectoryChangesW failed on " + finfo.file + ", handle "
                    + handle + ": '" + Kernel32Utils.getSystemError(err) + "' (" + err + ")");
        }
    }

    protected synchronized void dispose() {
        // unwatch any remaining files in map
        for (Iterator<File> i = watched.keySet().iterator(); i.hasNext();) {
            removeWatch(i.next());
        }
        // TODO double check that all handles are closed
        if (watcher != null) {
            watcher.interrupt();
            watcher = null;
        }

        if (port != null) {
            Kernel32 klib = Kernel32.INSTANCE;
            klib.PostQueuedCompletionStatus(port, 0, null, null);
            klib.CloseHandle(port);
            port = null;
        }
    }

    protected void finalize() {
        dispose();
    }

    public void addListener(EventListener<W32NotifyActionEvent> eventListener) {
        listeners.addListener(eventListener);
    }

    public boolean removeListener(EventListener<W32NotifyActionEvent> eventListener) {
        return listeners.removeListener(eventListener);
    }

    private final class EventPoller extends Thread {
        public EventPoller(String name) {
            super(name);
        }

        public void run() {
            FileInfo finfo;
            while (!isInterrupted()) {
                finfo = waitForChange();

                try {
                    // TODO handle asynchronously
                    handleChanges(finfo);
                } catch (IOException e) {
                    // TODO: how is this best handled?
                    e.printStackTrace();
                }
            }
        }

        private FileInfo waitForChange() {
            Kernel32 klib = Kernel32.INSTANCE;
            IntByReference rcount = new IntByReference();
            HANDLEByReference rkey = new HANDLEByReference();
            PointerByReference roverlap = new PointerByReference();
            klib.GetQueuedCompletionStatus(port, rcount, rkey, roverlap, Kernel32.INFINITE);

            synchronized (this) {
                return handleMap.get(rkey.getValue());
            }
        }

        private void handleChanges(FileInfo finfo) throws IOException {
            Kernel32 klib = Kernel32.INSTANCE;
            FILE_NOTIFY_INFORMATION fni = finfo.info;
            // Need an explicit read, since data was filled in asynchronously
            fni.read();
            do {
                File file = new File(finfo.file, fni.getFilename());
                W32NotifyActionEvent event = new W32NotifyActionEvent(fni.Action, file
                        .getAbsolutePath());
                // TODO broadcast asynchronously
                listeners.broadcast(event);
                fni = fni.next();
            } while (fni != null);
            // Trigger the next read
            if (!finfo.file.exists()) {
                removeWatch(finfo.file);
                return;
            }

            if (!klib.ReadDirectoryChangesW(finfo.handle, finfo.info, finfo.info.size(),
                    finfo.recursive, finfo.notifyMask, finfo.infoLength, finfo.overlapped, null)) {
                int err = klib.GetLastError();
                throw new IOException("ReadDirectoryChangesW failed on " + finfo.file + ": '"
                        + Kernel32Utils.getSystemError(err) + "' (" + err + ")");
            }
        }
    }

    private class FileInfo {
        private static final int BUFFER_SIZE = 4096;

        public final File file;

        public final HANDLE handle;

        public final int notifyMask;

        public final boolean recursive;

        public final FILE_NOTIFY_INFORMATION info = new FILE_NOTIFY_INFORMATION(BUFFER_SIZE);

        public final IntByReference infoLength = new IntByReference();

        public final OVERLAPPED overlapped = new OVERLAPPED();

        public FileInfo(File f, HANDLE h, int mask, boolean recurse) {
            this.file = f;
            this.handle = h;
            this.notifyMask = mask;
            this.recursive = recurse;
        }
    }
}