package com.limegroup.gnutella.library.monitor.poll;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

import com.google.inject.internal.base.Objects;
import com.limegroup.gnutella.library.monitor.FileMonitor;
import com.limegroup.gnutella.library.monitor.FileMonitorEvent;
import com.limegroup.gnutella.library.monitor.FileMonitorEventType;

public class PollingFileMonitor implements FileMonitor {
    private final EventListenerList<FileMonitorEvent> listeners;

    // TODO locks
    private final ConcurrentHashMap<String, WatchFile> watchTrees;

    public PollingFileMonitor() {
        listeners = new EventListenerList<FileMonitorEvent>();
        watchTrees = new ConcurrentHashMap<String, WatchFile>();
    }

    @Override
    public void addListener(EventListener<FileMonitorEvent> listener) {
        listeners.addListener(listener);

    }

    @Override
    public void addWatch(File file) throws IOException {
        addWatch(file, false);
    }

    @Override
    public void addWatch(File file, boolean recursive) throws IOException {
        if (file.exists()) {
            String path = file.getAbsolutePath();
            watchTrees.put(path, WatchFile.createWatchFile(path, recursive));
        }
    }

    @Override
    public void dispose() {

    }

    @Override
    public void init() throws IOException {

    }

    @Override
    public boolean removeListener(EventListener<FileMonitorEvent> listener) {
        return listeners.removeListener(listener);
    }

    @Override
    public void removeWatch(File file) throws IOException {
        watchTrees.remove(file.getAbsolutePath());
    }

    private static class WatchFile {
        private final List<WatchFile> list;

        private final String path;

        private final boolean recursive;

        private final long modifyTime;

        private final long size;

        private WatchFile(String path, boolean recursive, long modifyTime, long size) {
            this.list = new ArrayList<WatchFile>();
            this.path = Objects.nonNull(path, "path");
            this.recursive = recursive;
            this.modifyTime = modifyTime;
            this.size = size;
        }

        public void add(WatchFile watchFile) {
            list.add(watchFile);
        }

        public String getPath() {
            return path;
        }

        public boolean isRecursive() {
            return recursive;
        }

        public List<WatchFile> list() {
            return list;
        }

        @Override
        public boolean equals(Object obj) {
            if (!WatchFile.class.isInstance(obj)) {
                return false;
            }

            WatchFile watchFile = (WatchFile) obj;
            return path.equals(watchFile.path);
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        public static WatchFile createWatchFile(String path, boolean recursive) {
            File file = new File(path);
            WatchFile watchFile = null;
            if (file.exists()) {
                watchFile = new WatchFile(file.getAbsolutePath(), recursive, file.lastModified(),
                        file.length());
                if (file.isDirectory() && recursive) {
                    for (File file2 : file.listFiles()) {
                        watchFile
                                .add(WatchFile.createWatchFile(file2.getAbsolutePath(), recursive));
                    }
                }
            }
            return watchFile;
        }

        public boolean contains(WatchFile watchFile) {
            return list.contains(watchFile);
        }

        public List<FileMonitorEvent> diff(WatchFile watchFile) {

            List<FileMonitorEvent> changes = new ArrayList<FileMonitorEvent>();

            if (watchFile == null) {
                changes.add(new FileMonitorEvent(FileMonitorEventType.DELETE, path));
            } else {

                if (size != watchFile.size || modifyTime != watchFile.modifyTime) {
                    changes.add(new FileMonitorEvent(FileMonitorEventType.UPDATE, path));
                }

                for (WatchFile file : list) {
                    if (watchFile.contains(file)) {
                        WatchFile fil = watchFile.get(file);
                        changes.addAll(file.diff(fil));
                    } else {
                        changes.add(new FileMonitorEvent(FileMonitorEventType.DELETE, file
                                .getPath()));

                    }
                }

                for (WatchFile file : watchFile.list) {
                    if (!contains(file)) {
                        changes.add(new FileMonitorEvent(FileMonitorEventType.CREATE, file
                                .getPath()));
                    }
                }
            }

            return changes;
        }

        private WatchFile get(WatchFile file) {
            return list.get(list.indexOf(file));
        }
    }

    public List<FileMonitorEvent> poll() {
        List<FileMonitorEvent> changes = new ArrayList<FileMonitorEvent>();
        for (WatchFile watchFile : watchTrees.values()) {
            WatchFile newWatch = WatchFile.createWatchFile(watchFile.getPath(), watchFile
                    .isRecursive());
            changes.addAll(watchFile.diff(newWatch));
            watchTrees.put(newWatch.getPath(), newWatch);
        }

        return changes;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        PollingFileMonitor pollingFileMonitor = new PollingFileMonitor();
        pollingFileMonitor.addWatch(new File("/home/pvertenten/test1"), true);
        pollingFileMonitor.addWatch(new File("/home/pvertenten/test2"), true);
        while (true) {
            poll(pollingFileMonitor);
            Thread.sleep(5000);
        }
    }

    private static void poll(PollingFileMonitor pollingFileMonitor) {
        List<FileMonitorEvent> changes = pollingFileMonitor.poll();
        if (changes.size() == 0) {
            System.out.println("no change");
        } else {
            for (FileMonitorEvent event : changes) {
                System.out.println(event);
            }
        }
    }
}
