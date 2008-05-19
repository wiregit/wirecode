package com.limegroup.gnutella.connection;

import java.util.EventObject;

public class ConnectionLifecycleEvent extends EventObject {
    
    /** Defines the various events during connection. */
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
    
    private final RoutedConnection connection;
    private final EventType type;
    
    public ConnectionLifecycleEvent(Object source, EventType type, RoutedConnection c) {
        super(source);
        this.connection = c;
        this.type = type;
    }

    /**
     * Constructs a ConnectionLifecycleEvent with no connection associated.
     * This is useful for CONNECTED, DISCONNECTED, NO_INTERNET and
     * ADDRESS_CHANGED events.
     * 
     */
    public ConnectionLifecycleEvent(Object source, EventType type) {
        this(source, type, null);
    }

    public EventType getType() {
        return type;
    }

    public RoutedConnection getConnection() {
        return connection;
    }

    public boolean isConnectingEvent() {
        return (type.equals(EventType.CONNECTING)); 
    }
    
    public boolean isConnectedEvent() {
        return (type.equals(EventType.CONNECTED));
    }
    
    public boolean isDisconnectedEvent() {
        return (type.equals(EventType.DISCONNECTED));
    }
    
    public boolean isNoInternetEvent() {
        return (type.equals(EventType.NO_INTERNET));
    }
    
    public boolean isConnectionInitializingEvent() {
        return (type.equals(EventType.CONNECTION_INITIALIZING));
    }
    
    public boolean isConnectionClosedEvent() {
        return (type.equals(EventType.CONNECTION_CLOSED));
    }
    
    public boolean isConnectionInitializedEvent() {
        return (type.equals(EventType.CONNECTION_INITIALIZED));
    }
    
    public boolean isConnectionCapabilitiesEvent() {
        return (type.equals(EventType.CONNECTION_CAPABILITIES));
    }
    
    @Override
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
