package com.limegroup.gnutella.dht;

import java.util.EventObject;

public class DHTEvent extends EventObject {
    
    public static enum Type {
        STARTING,
        CONNECTED,
        STOPPED;
    }
    
    private final Type type;

    public DHTEvent(Object source, Type type) {
        super(source);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
