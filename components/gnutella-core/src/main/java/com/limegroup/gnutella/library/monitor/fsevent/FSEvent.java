package com.limegroup.gnutella.library.monitor.fsevent;


public class FSEvent {

    private final String path;

    private final int eventId;

    private final int eventFlag;
    public FSEvent(String path, int eventId, int eventFlag) {
        this.path = path;
        this.eventId = eventId;
        this.eventFlag = eventFlag;
    }

    public String getPath() {
        return path;
    }

    public int getEventId() {
        return eventId;
    }

    @Override
    public String toString() {
        return eventId + " -  " + eventFlag + " - " + path;
    }
}
