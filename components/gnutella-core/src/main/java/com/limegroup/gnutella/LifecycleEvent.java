package com.limegroup.gnutella;

import java.util.EventObject;

public class LifecycleEvent extends EventObject {
    
    public static final int CONNECTING = 1;
    public static final int CONNECTED = 2;
    public static final int DISCONNECTED = 3;
    public static final int NO_INTERNET = 4;
    public static final int CONNECTION_INITIALIZING = 6;
    public static final int CONNECTION_INITIALIZED = 7;
    public static final int CONNECTION_CLOSED = 8;
   
    private final Connection connection;
    private final int type;
    
    public LifecycleEvent(Object source, int type, Connection c) {
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
    public LifecycleEvent(Object source, int type) {
        this(source, type, null);
    }

    public int getType() {
        return type;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isConnectingEvent() {
        return (type == CONNECTING); 
    }
    
    public boolean isConnectedEvent() {
        return (type == CONNECTED);
    }
    
    public boolean isDisconnectedEvent() {
        return (type == DISCONNECTED);
    }
    
    public boolean isNoInternetEvent() {
        return (type == NO_INTERNET);
    }
    
    public boolean isConnectionInitializingEvent() {
        return (type == CONNECTION_INITIALIZING);
    }
    
    public boolean isConnectionClosedEvent() {
        return (type == CONNECTION_CLOSED);
    }
    
    public boolean isConnectionInitializedEvent() {
        return (type == CONNECTION_INITIALIZED);
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("LifecycleEvent: [event=");
    
        switch(type) {
            case CONNECTING:
                buffer.append("ADD");
                break;
            case CONNECTED:
                buffer.append("CONNECTED");
                break;
            case DISCONNECTED:
                buffer.append("DISCONNECTED");
                break;
            case NO_INTERNET:
                buffer.append("NO_INTERNET");
                break;
            case CONNECTION_INITIALIZING:
                buffer.append("CONNECTION_INITIALIZING");
                break;
            case CONNECTION_INITIALIZED:
                buffer.append("CONNECTION_INITIALIZED");
                break;
            case CONNECTION_CLOSED:
                buffer.append("CONNECTION_CLOSED");
                break;
            default:
                buffer.append("UNKNOWN");
                break;
        }
        buffer.append(", connection=");
        if(connection != null) {
            buffer.append(connection.toString());
        } else {
            buffer.append(", connection= null");
        }
        return buffer.append("]").toString();
    }
}
