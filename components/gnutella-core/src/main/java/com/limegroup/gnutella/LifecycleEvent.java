package com.limegroup.gnutella;

import java.util.EventObject;

public class LifecycleEvent extends EventObject {
    
    public static enum LifeEvent {
        CONNECTING("connecting"), 
        CONNECTED("connected"), 
        DISCONNECTED("disconnected"), 
        NO_INTERNET("no_internet"), 
        CONNECTION_INITIALIZING("connection_initializing"), 
        CONNECTION_INITIALIZED("connection_initialized"), 
        CONNECTION_CLOSED("connection_closed"),
        CONNECTION_VENDORED("connection_vendored");
        
        private final String name;
        
        LifeEvent(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    private final Connection connection;
    private final LifeEvent type;
    
    public LifecycleEvent(Object source, LifeEvent type, Connection c) {
        super(source);
        this.connection = c;
        this.type = type;
    }

    /**
     * Constructs a LifeCycleEvent with no connection associated.
     * This is usefull for CONNECTED, DISCONNECTED, NO_INTERNET and
     * ADDRESS_CHANGED events
     * 
     * @param manager
     */
    public LifecycleEvent(Object source, LifeEvent type) {
        this(source, type, null);
    }

    public LifeEvent getType() {
        return type;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isConnectingEvent() {
        return (type == LifeEvent.CONNECTING); 
    }
    
    public boolean isConnectedEvent() {
        return (type == LifeEvent.CONNECTED);
    }
    
    public boolean isDisconnectedEvent() {
        return (type == LifeEvent.DISCONNECTED);
    }
    
    public boolean isNoInternetEvent() {
        return (type == LifeEvent.NO_INTERNET);
    }
    
    public boolean isConnectionInitializingEvent() {
        return (type == LifeEvent.CONNECTION_INITIALIZING);
    }
    
    public boolean isConnectionClosedEvent() {
        return (type == LifeEvent.CONNECTION_CLOSED);
    }
    
    public boolean isConnectionInitializedEvent() {
        return (type == LifeEvent.CONNECTION_INITIALIZED);
    }
    
    public boolean isConnectionVendoredEvent() {
        return (type == LifeEvent.CONNECTION_VENDORED);
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("LifecycleEvent: [event=");
        buffer.append(type.getName());
        buffer.append(", connection=");
        if(connection != null) {
            buffer.append(connection.toString());
        } else {
            buffer.append(", connection= null");
        }
        return buffer.append("]").toString();
    }
}
