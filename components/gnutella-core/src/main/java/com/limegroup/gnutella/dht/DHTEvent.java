package com.limegroup.gnutella.dht;

import java.util.EventObject;

public class DHTEvent extends EventObject {
    
    public static enum Type {
        STARTING,
        CONNECTED,
        STOPPED;
    }
    
    private final Type type;

    public DHTEvent(DHTController source, Type type) {
        super(source);
        this.type = type;
    }

    public DHTController getDHTController() {
        return (DHTController)getSource();
    }
    
    public Type getType() {
        return type;
    }
    
    public String toString() {
        return type.toString();
    }
}
