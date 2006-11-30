package com.limegroup.gnutella.connection;

import java.util.EventObject;

import com.limegroup.gnutella.ManagedConnection;

public class ConnectionLifecycleEvent extends EventObject {
    
    public static enum ConnectionLifeEvent {
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
    private final ConnectionLifeEvent type;
    
    public ConnectionLifecycleEvent(Object source, ConnectionLifeEvent type, ManagedConnection c) {
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
    public ConnectionLifecycleEvent(Object source, ConnectionLifeEvent type) {
        this(source, type, null);
    }

    public ConnectionLifeEvent getType() {
        return type;
    }

    public ManagedConnection getConnection() {
        return connection;
    }

    public boolean isConnectingEvent() {
        return (type == ConnectionLifeEvent.CONNECTING); 
    }
    
    public boolean isConnectedEvent() {
        return (type == ConnectionLifeEvent.CONNECTED);
    }
    
    public boolean isDisconnectedEvent() {
        return (type == ConnectionLifeEvent.DISCONNECTED);
    }
    
    public boolean isNoInternetEvent() {
        return (type == ConnectionLifeEvent.NO_INTERNET);
    }
    
    public boolean isConnectionInitializingEvent() {
        return (type == ConnectionLifeEvent.CONNECTION_INITIALIZING);
    }
    
    public boolean isConnectionClosedEvent() {
        return (type == ConnectionLifeEvent.CONNECTION_CLOSED);
    }
    
    public boolean isConnectionInitializedEvent() {
        return (type == ConnectionLifeEvent.CONNECTION_INITIALIZED);
    }
    
    public boolean isConnectionCapabilitiesEvent() {
        return (type == ConnectionLifeEvent.CONNECTION_CAPABILITIES);
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
