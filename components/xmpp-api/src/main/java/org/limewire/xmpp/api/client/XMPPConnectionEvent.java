package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultSourceTypeEvent;
import org.limewire.core.api.friend.client.FriendConnection;

public class XMPPConnectionEvent extends DefaultSourceTypeEvent<FriendConnection, XMPPConnectionEvent.Type> {

    public static enum Type {
        CONNECTING, CONNECTED, CONNECT_FAILED, DISCONNECTED
    }
    
    private final Exception exception;
    
    public XMPPConnectionEvent(FriendConnection source, XMPPConnectionEvent.Type event) {
        this(source, event, null);
    }

    public XMPPConnectionEvent(FriendConnection source, XMPPConnectionEvent.Type event, Exception exception) {
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
