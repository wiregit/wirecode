package com.limegroup.gnutella.library.monitor.fsevent;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.monitor.FileMonitor;
import com.limegroup.gnutella.library.monitor.FileMonitorEvent;
import com.limegroup.gnutella.library.monitor.FileMonitorEventType;

public class FileMonitorMac implements FileMonitor {

    private final FSEventMonitor fsEventMonitor;

    private final ConcurrentHashMap<EventListener<FileMonitorEvent>, EventListener<FSEvent>> listeners;

    public FileMonitorMac() {
        this.fsEventMonitor = new FSEventMonitor();
        this.listeners = new ConcurrentHashMap<EventListener<FileMonitorEvent>, EventListener<FSEvent>>();
    }

    @Override
    public void init() throws IOException {
        fsEventMonitor.init();
    }

    @Override
    public void addListener(EventListener<FileMonitorEvent> listener) {
        EventListener<FSEvent> fsEventListener = new FSEventAdapter(listener);
        listeners.put(listener, fsEventListener);
        fsEventMonitor.addListener(fsEventListener);
    }

    @Override
    public void addWatch(File file) throws IOException {
        addWatch(file, true);
    }

    @Override
    public void addWatch(File file, boolean recursive) throws IOException {
        if (file.isDirectory()) {
            fsEventMonitor.addWatch(file);
        } else {
            fsEventMonitor.addWatch(file.getParentFile());
        }
    }

    @Override
    public void dispose() {
        fsEventMonitor.dispose();
    }

    @Override
    public boolean removeListener(EventListener<FileMonitorEvent> listener) {
        return fsEventMonitor.removeListener(listeners.get(listener));
    }

    @Override
    public void removeWatch(File file) throws IOException {
        if (file.isDirectory()) {
            fsEventMonitor.removeWatch(file);
        } else {
            fsEventMonitor.removeWatch(file.getParentFile());
        }

    }

    private final class FSEventAdapter implements EventListener<FSEvent> {
        private final EventListener<FileMonitorEvent> listener;

        public FSEventAdapter(EventListener<FileMonitorEvent> listener) {
            this.listener = listener;
        }

        @Override
        public void handleEvent(FSEvent event) {
            FileMonitorEvent fileMonitorEvent = translate(event);
            if (fileMonitorEvent != null) {
                listener.handleEvent(fileMonitorEvent);
            }
        }

        private FileMonitorEvent translate(FSEvent event) {
            String path = event.getPath();
            FileMonitorEvent fileMonitorEvent = null;
            fileMonitorEvent = new FileMonitorEvent(FileMonitorEventType.UPDATE, path);
            return fileMonitorEvent;
        }
    }

}
