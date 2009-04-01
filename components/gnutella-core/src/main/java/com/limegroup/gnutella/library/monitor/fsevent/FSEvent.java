package com.limegroup.gnutella.library.monitor.fsevent;


public class FSEvent {

    private final String path;

    private final int eventId;

    public FSEvent(String path, int eventId) {
        this.path = path;
        this.eventId = eventId;
    }

    public String getPath() {
        return path;
    }

    public int getEventId() {
        return eventId;
    }

    @Override
    public String toString() {
        return eventId + " - " + path;
    }
}
