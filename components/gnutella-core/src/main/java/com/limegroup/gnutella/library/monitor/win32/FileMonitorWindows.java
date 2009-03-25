package com.limegroup.gnutella.library.monitor.win32;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.listener.EventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.monitor.FileMonitor;
import com.limegroup.gnutella.library.monitor.FileMonitorEvent;
import com.limegroup.gnutella.library.monitor.FileMonitorEventType;

@Singleton
public class FileMonitorWindows implements FileMonitor {
    private final Win32FileMonitor fileMonitor;

    private final ConcurrentHashMap<EventListener<FileMonitorEvent>, EventListener<W32NotifyActionEvent>> listeners;

    @Inject
    public FileMonitorWindows() {
        this.fileMonitor = new Win32FileMonitor();
        listeners = new ConcurrentHashMap<EventListener<FileMonitorEvent>, EventListener<W32NotifyActionEvent>>();
    }

    @Inject
    @Override
    public void init() throws IOException {
        // TODO initialize as service instead
        fileMonitor.init();

    }

    @Override
    public void addListener(EventListener<FileMonitorEvent> listener) {
        EventListener<W32NotifyActionEvent> w32NotifyListener = new W32NotifyActionEventAdapter(
                listener);
        listeners.put(listener, w32NotifyListener);
        fileMonitor.addListener(w32NotifyListener);
    }

    @Override
    public void addWatch(File file) throws IOException {
        fileMonitor.addWatch(file);
    }

    @Override
    public void addWatch(File file, boolean recursive) throws IOException {
        fileMonitor.addWatch(file, recursive);
    }

    @Override
    public void dispose() {
        fileMonitor.dispose();
    }

    @Override
    public boolean removeListener(EventListener<FileMonitorEvent> listener) {
        return fileMonitor.removeListener(listeners.get(listener));
    }

    @Override
    public void removeWatch(File file) throws IOException {
        fileMonitor.removeWatch(file);
    }

    private final class W32NotifyActionEventAdapter implements EventListener<W32NotifyActionEvent> {
        private final EventListener<FileMonitorEvent> listener;

        public W32NotifyActionEventAdapter(EventListener<FileMonitorEvent> listener) {
            this.listener = listener;
        }

        @Override
        public void handleEvent(W32NotifyActionEvent event) {
            FileMonitorEvent fileMonitorEvent = translate(event);
            if (fileMonitorEvent != null) {
                listener.handleEvent(fileMonitorEvent);
            }
        }

        private FileMonitorEvent translate(W32NotifyActionEvent event) {
            String path = event.getPath();
            FileMonitorEvent fileMonitorEvent = null;
            if (event.isModify()) {
                fileMonitorEvent = new FileMonitorEvent(FileMonitorEventType.UPDATE, path);
            } else if (event.isCreate() || event.isRenamedNewName()) {
                fileMonitorEvent = new FileMonitorEvent(FileMonitorEventType.CREATE, path);
            } else if (event.isDelete() || event.isRenamedOldName()) {
                fileMonitorEvent = new FileMonitorEvent(FileMonitorEventType.DELETE, path);
            } else {
                // TODO handle other events?
            }
            return fileMonitorEvent;
        }
    }
}
