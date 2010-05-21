package com.limegroup.gnutella.dht2;

import java.util.EventObject;

/**
 * {@link DHTEvent}s are fired for DHT state changes.
 */
public class DHTEvent extends EventObject {

    private static final long serialVersionUID = 912814275883336092L;

    /**
     * Defines the various type of <code>DHTEvent</code>s, either starting, 
     * connected or stopped.
     */
    public static enum Type {
        STARTING,
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
    
    public DHTManager getManager() {
        return (DHTManager)getSource();
    }
    
    @Override
    public String toString() {
        return type.toString();
    }
}
