package com.limegroup.gnutella.connection;

import java.util.EventObject;

import com.limegroup.gnutella.ManagedConnection;

public class ConnectionLifecycleEvent extends EventObject {
    
    public static enum EventType {
        CONNECTING, 
        CONNECTED, 
        DISCONNECTED, 
        NO_INTERNET, 
        CONNECTION_INITIALIZING, 
        CONNECTION_INITIALIZED, 
        CONNECTION_CLOSED,
        CONNECTION_CAPABILITIES;
    }
    
    private final ManagedConnection connection;
    private final EventType type;
    
    public ConnectionLifecycleEvent(Object source, EventType type, ManagedConnection c) {
        super(source);
        this.connection = c;
        this.type = type;
    }

    /**
     * Constructs a ConnectionLifecycleEvent with no connection associated.
     * This is usefull for CONNECTED, DISCONNECTED, NO_INTERNET and
     * ADDRESS_CHANGED events
     * 
     * @param manager
     */
    public ConnectionLifecycleEvent(Object source, EventType type) {
        this(source, type, null);
    }

    public EventType getType() {
        return type;
    }

    public ManagedConnection getConnection() {
        return connection;
    }

    public boolean isConnectingEvent() {
        return (type == EventType.CONNECTING); 
    }
    
    public boolean isConnectedEvent() {
        return (type == EventType.CONNECTED);
    }
    
    public boolean isDisconnectedEvent() {
        return (type == EventType.DISCONNECTED);
    }
    
    public boolean isNoInternetEvent() {
        return (type == EventType.NO_INTERNET);
    }
    
    public boolean isConnectionInitializingEvent() {
        return (type == EventType.CONNECTION_INITIALIZING);
    }
    
    public boolean isConnectionClosedEvent() {
        return (type == EventType.CONNECTION_CLOSED);
    }
    
    public boolean isConnectionInitializedEvent() {
        return (type == EventType.CONNECTION_INITIALIZED);
    }
    
    public boolean isConnectionCapabilitiesEvent() {
        return (type == EventType.CONNECTION_CAPABILITIES);
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("ConnectionLifecycleEvent: [event=");
        buffer.append(type);
        buffer.append(", connection=");
        if(connection != null) {
            buffer.append(connection.toString());
        } else {
            buffer.append(", connection= null");
        }
        return buffer.append("]").toString();
    }
}
