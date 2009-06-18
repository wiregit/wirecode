package org.limewire.friend.api;

import org.limewire.listener.DefaultSourceTypeEvent;

public class FriendConnectionEvent extends DefaultSourceTypeEvent<FriendConnection, FriendConnectionEvent.Type> {

    public static enum Type {
        CONNECTING, CONNECTED, CONNECT_FAILED, DISCONNECTED
    }
    
    private final Exception exception;
    
    public FriendConnectionEvent(FriendConnection source, FriendConnectionEvent.Type event) {
        this(source, event, null);
    }

    public FriendConnectionEvent(FriendConnection source, FriendConnectionEvent.Type event, Exception exception) {
        super(source, event);
        this.exception = exception;
    }
    
    /**
     * @return null if there is no exception that was the cause of this event
     */
    public Exception getException() {
        return exception;
    }
}
