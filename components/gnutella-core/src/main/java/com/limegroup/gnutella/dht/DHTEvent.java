package com.limegroup.gnutella.dht;

import java.util.EventObject;

public class DHTEvent extends EventObject {
    
    public static enum EventType {
        STARTING,
        CONNECTED,
        STOPPED;
    }
    
    private final EventType type;

    public DHTEvent(Object source, EventType type) {
        super(source);
        this.type = type;
    }

    public EventType getType() {
        return type;
    }
}
