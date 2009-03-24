package com.limegroup.gnutella.library.monitor;

public class FileMonitorEvent {
    private final FileMonitorEventType type;

    private final String path;

    public FileMonitorEvent(FileMonitorEventType type, String path) {
        this.type = type;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public FileMonitorEventType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + ": " + path;
    }
}
