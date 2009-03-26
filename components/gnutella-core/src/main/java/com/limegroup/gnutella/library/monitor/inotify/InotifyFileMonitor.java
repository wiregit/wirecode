package com.limegroup.gnutella.library.monitor.inotify;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public class InotifyFileMonitor {

    private final INotify iNotify;

    // TODO use a BIDI map instead
    private final Map<String, Integer> fileWatchDescriptors;

    private final Map<Integer, String> watchDescriptorFiles;

    private final EventListenerList<INotifyEvent> listeners;

    private int watchHandle = -1;
    
    private Thread watcher = null;

    public InotifyFileMonitor() {
        listeners = new EventListenerList<INotifyEvent>();
        fileWatchDescriptors = new ConcurrentHashMap<String, Integer>();
        watchDescriptorFiles = new ConcurrentHashMap<Integer, String>();
        iNotify = (INotify) Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"),
                INotify.class);
    }

    public synchronized void init() throws IOException {
        if (watchHandle == -1) {
            watchHandle = iNotify.inotify_init();
            if (watchHandle == -1) {
                throw new IOException("inotify subsystem failed initialization.");
            } else {
                watcher = new Thread(new EventPoller());
                watcher.start();
            }
        }
    }

    public synchronized void addWatch(File file) throws IOException {
        String path = file.getAbsolutePath();
        Integer watchDescriptor = iNotify.inotify_add_watch(watchHandle, path,
                INotifyEventMask.ALL_EVENTS.getMask());
        fileWatchDescriptors.put(path, watchDescriptor);
        watchDescriptorFiles.put(watchDescriptor, path);
    }

    public synchronized void removeWatch(File file) throws IOException {
        String path = file.getAbsolutePath();
        Integer watchDescriptor = fileWatchDescriptors.get(path);
        iNotify.inotify_rm_watch(watchHandle, watchDescriptor);
        // TODO handler error cases
        fileWatchDescriptors.remove(path);
        watchDescriptorFiles.remove(watchDescriptor);
    }

    private class EventPoller implements Runnable {
        @Override
        public void run() {
            Memory p = new Memory(32 * 1024 * 1024);

            while (watchHandle != -1) {
                int count = iNotify.read(watchHandle, p, (int) p.getSize());
                if (count == -1) {
                    // TODO some kind of error
                    return;
                }

                int consumed = 0;
                while (consumed < count) {
                    INotifyEvent iNotifyEvent = new INotifyEvent();
                    consumed += iNotifyEvent.readStruct(p, consumed, watchDescriptorFiles);
                    // TODO broadcast this asynchronously, missing events
                    // otherwise?
                    listeners.broadcast(iNotifyEvent);
                }
            }
        }
    }

    public void addListener(EventListener<INotifyEvent> listener) {
        listeners.addListener(listener);
    }

    public boolean removeListener(EventListener<INotifyEvent> listener) {
        return listeners.removeListener(listener);
    }

    public void dispose() {
        // TODO dispose of resources
    }

}
