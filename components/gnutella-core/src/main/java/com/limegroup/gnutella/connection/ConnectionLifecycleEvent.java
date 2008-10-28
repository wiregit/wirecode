package com.limegroup.gnutella.connection;

import java.util.EventObject;

import org.limewire.core.api.connection.ConnectionLifeCycleEventType;

public class ConnectionLifecycleEvent extends EventObject {
    
    private final RoutedConnection connection;
    private final ConnectionLifeCycleEventType type;
    
    public ConnectionLifecycleEvent(Object source, ConnectionLifeCycleEventType type, RoutedConnection c) {
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
    public ConnectionLifecycleEvent(Object source, ConnectionLifeCycleEventType type) {
        this(source, type, null);
    }

    public ConnectionLifeCycleEventType getType() {
        return type;
    }

    public RoutedConnection getConnection() {
        return connection;
    }

    public boolean isConnectingEvent() {
        return (type.equals(ConnectionLifeCycleEventType.CONNECTING)); 
    }
    
    public boolean isConnectedEvent() {
        return (type.equals(ConnectionLifeCycleEventType.CONNECTED));
    }
    
    public boolean isDisconnectedEvent() {
        return (type.equals(ConnectionLifeCycleEventType.DISCONNECTED));
    }
    
    public boolean isNoInternetEvent() {
        return (type.equals(ConnectionLifeCycleEventType.NO_INTERNET));
    }
    
    public boolean isConnectionInitializingEvent() {
        return (type.equals(ConnectionLifeCycleEventType.CONNECTION_INITIALIZING));
    }
    
    public boolean isConnectionClosedEvent() {
        return (type.equals(ConnectionLifeCycleEventType.CONNECTION_CLOSED));
    }
    
    public boolean isConnectionInitializedEvent() {
        return (type.equals(ConnectionLifeCycleEventType.CONNECTION_INITIALIZED));
    }
    
    public boolean isConnectionCapabilitiesEvent() {
        return (type.equals(ConnectionLifeCycleEventType.CONNECTION_CAPABILITIES));
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
