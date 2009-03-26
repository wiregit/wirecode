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
import com.limegroup.gnutella.library.monitor.win32.api.Kernel32Interface;
import com.limegroup.gnutella.library.monitor.win32.api.OVERLAPPED;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class Win32FileMonitor {
    private final Map<File, Integer> watched;

    private final Map<File, FileInfo> fileMap;

    private final Map<HANDLE, FileInfo> handleMap;

    private final EventListenerList<W32NotifyActionEvent> listeners;

    private final Kernel32 kernel32;

    private HANDLE port;

    private Thread watcher;

    public Win32FileMonitor() {
        kernel32 = new Kernel32();
        watched = new HashMap<File, Integer>();
        fileMap = new HashMap<File, FileInfo>();
        handleMap = new HashMap<HANDLE, FileInfo>();
        listeners = new EventListenerList<W32NotifyActionEvent>();
    }

    public synchronized void init() throws IOException {
        if (port == null) {
            port = kernel32.CreateIoCompletionPort(INVALID_HANDLE_VALUE.INVALID_HANDLE, port, null,
                    0);
            if (port == null) {
                int err = kernel32.GetLastError();
                throw new IOException("Error initializing IOCompletionPort: '"
                        + kernel32.getSystemError(err) + "' (" + err + ")");
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
            kernel32.CloseHandle(finfo.handle);
        }
    }

    private synchronized void watch(File file, int eventMask, boolean recursive) throws IOException {
        if (port == null) {
            throw new IOException("Cannot add watches to the FileMonitor before it is initialized.");
        }
        int mask = Kernel32Interface.FILE_SHARE_READ | Kernel32Interface.FILE_SHARE_WRITE
                | Kernel32Interface.FILE_SHARE_DELETE;
        int flags = Kernel32Interface.FILE_FLAG_BACKUP_SEMANTICS
                | Kernel32Interface.FILE_FLAG_OVERLAPPED;
        HANDLE handle = kernel32.CreateFile(file.getAbsolutePath(),
                Kernel32Interface.FILE_LIST_DIRECTORY, mask, null, Kernel32Interface.OPEN_EXISTING,
                flags, null);
        if (INVALID_HANDLE_VALUE.INVALID_HANDLE.equals(handle)) {
            throw new IOException("Unable to open " + file + " (" + kernel32.GetLastError() + ")");
        }
        FileInfo finfo = new FileInfo(file, handle, eventMask, recursive);
        watched.put(file, new Integer(eventMask));
        fileMap.put(file, finfo);
        handleMap.put(handle, finfo);
        // Existing port is returned
        port = kernel32.CreateIoCompletionPort(handle, port, handle.getPointer(), 0);
        if (INVALID_HANDLE_VALUE.INVALID_HANDLE.equals(port)) {
            // TODO remove from apps if exception?
            throw new IOException("Unable to create/use I/O Completion port " + "for " + file
                    + " (" + kernel32.GetLastError() + ")");
        }

        if (!kernel32.ReadDirectoryChangesW(handle, finfo.info, finfo.info.size(), recursive,
                eventMask, finfo.infoLength, finfo.overlapped, null)) {
            int err = kernel32.GetLastError();
            // TODO remove from apps if exception?
            throw new IOException("ReadDirectoryChangesW failed on " + finfo.file + ", handle "
                    + handle + ": '" + kernel32.getSystemError(err) + "' (" + err + ")");
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
            kernel32.PostQueuedCompletionStatus(port, 0, null, null);
            kernel32.CloseHandle(port);
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
            IntByReference rcount = new IntByReference();
            HANDLEByReference rkey = new HANDLEByReference();
            PointerByReference roverlap = new PointerByReference();
            kernel32.GetQueuedCompletionStatus(port, rcount, rkey, roverlap,
                    Kernel32Interface.INFINITE);

            synchronized (this) {
                return handleMap.get(rkey.getValue());
            }
        }

        private void handleChanges(FileInfo finfo) throws IOException {
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

            if (!kernel32.ReadDirectoryChangesW(finfo.handle, finfo.info, finfo.info.size(),
                    finfo.recursive, finfo.notifyMask, finfo.infoLength, finfo.overlapped, null)) {
                int err = kernel32.GetLastError();
                throw new IOException("ReadDirectoryChangesW failed on " + finfo.file + ": '"
                        + kernel32.getSystemError(err) + "' (" + err + ")");
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