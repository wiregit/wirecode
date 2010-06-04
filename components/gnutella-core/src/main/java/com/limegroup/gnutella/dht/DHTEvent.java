package com.limegroup.gnutella.dht;

import java.util.EventObject;

/**
 * {@link DHTEvent}s are fired for DHT state changes.
 */
public class DHTEvent extends EventObject {

    private static final long serialVersionUID = 912814275883336092L;

    /**
     * Defines the various type of {@link DHTEvent}s, either starting, 
     * connecting, connected or stopped.
     */
    public static enum Type {
        STARTING,
        CONNECTING,
        CONNECTED,
        STOPPED;
    }
    
    private final Type type;
    
    public DHTEvent(Type type, DHTManager manager) {
        super(manager);
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
    
    /**
     * Returns the {@link DHTManager}.
     */
    public DHTManager getManager() {
        return (DHTManager)getSource();
    }
    
    @Override
    public String toString() {
        return type.toString();
    }
}
