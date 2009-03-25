package com.limegroup.gnutella.library.monitor.inotify;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.monitor.FileMonitor;
import com.limegroup.gnutella.library.monitor.FileMonitorEvent;
import com.limegroup.gnutella.library.monitor.FileMonitorEventType;

public class FileMonitorLinux implements FileMonitor {
    private final InotifyFileMonitor inotifyFileMonitor;

    private final ConcurrentHashMap<EventListener<FileMonitorEvent>, EventListener<INotifyEvent>> listeners;

    private final ConcurrentHashMap<String, Boolean> watches;

    public FileMonitorLinux() {
        this.inotifyFileMonitor = new InotifyFileMonitor();
        this.listeners = new ConcurrentHashMap<EventListener<FileMonitorEvent>, EventListener<INotifyEvent>>();
        this.watches = new ConcurrentHashMap<String, Boolean>();
    }

    public void addListener(final EventListener<FileMonitorEvent> listener) {
        EventListener<INotifyEvent> eventListener = new INotifyEventAdapter(listener);
        inotifyFileMonitor.addListener(eventListener);
    }

    public void addWatch(File file) throws IOException {
        addWatch(file, false);
    }

    @Override
    public void addWatch(File file, boolean recursive) throws IOException {
        // TODO see about making this non recursive
        watches.put(file.getAbsolutePath(), recursive);
        inotifyFileMonitor.addWatch(file);
        if (recursive && file.isDirectory()) {
            for (File filei : file.listFiles()) {
                if (filei.isDirectory()) {
                    addWatch(filei, true);
                }
            }
        }
    }

    public void init() throws IOException {
        inotifyFileMonitor.init();
    }

    public boolean removeListener(EventListener<FileMonitorEvent> listener) {
        return inotifyFileMonitor.removeListener(listeners.get(listener));
    }

    public void removeWatch(File file) throws IOException {
        inotifyFileMonitor.removeWatch(file);
    }

    public void dispose() {
        inotifyFileMonitor.dispose();
        listeners.clear();
    }

    private final class INotifyEventAdapter implements EventListener<INotifyEvent> {
        private final EventListener<FileMonitorEvent> listener;

        private INotifyEventAdapter(EventListener<FileMonitorEvent> listener) {
            this.listener = listener;
        }

        @Override
        public void handleEvent(INotifyEvent event) {
            FileMonitorEvent fileMonitorEvent = translate(event);
            if (fileMonitorEvent != null) {
                String path = event.getFullPath();
                File file = new File(path);
                // TODO manage queue better
                if (file.isDirectory() && event.isCreateEvent()) {
                    String watchPath = event.getWatchPath();
                    Boolean recursive = watches.get(watchPath);
                    if (recursive != null && recursive.booleanValue()) {
                        try {
                            // TODO doesn't really work, because there is always
                            // the potential to have some events come in, before
                            // a watch is created on the root directory. especially 
                            //in the event of a mv or what have you.
                            addWatch(file, true);
                        } catch (IOException e) {
                            // TODO handle better
                            throw new RuntimeException(e);
                        }
                    }
                }
                listener.handleEvent(fileMonitorEvent);
            }
        }

        private FileMonitorEvent translate(INotifyEvent event) {
            String path = event.getFullPath();
            FileMonitorEvent fileMonitorEvent = null;
            if (event.isCloseWriteEvent() || event.isModifyEvent() || event.isMovedSelfEvent()) {
                fileMonitorEvent = new FileMonitorEvent(FileMonitorEventType.UPDATE, path);
            } else if (event.isCreateEvent() || event.isMovedToEvent()) {
                fileMonitorEvent = new FileMonitorEvent(FileMonitorEventType.CREATE, path);
            } else if (event.isDeleteEvent() || event.isDeleteSelfEvent()
                    || event.isMovedFromEvent()) {
                fileMonitorEvent = new FileMonitorEvent(FileMonitorEventType.DELETE, path);
            }
            return fileMonitorEvent;
        }
    }
}
