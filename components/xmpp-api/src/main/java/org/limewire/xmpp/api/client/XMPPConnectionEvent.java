package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultEvent;

public class XMPPConnectionEvent extends DefaultEvent<XMPPConnection, XMPPConnectionEvent.Type> {

    public static enum Type {
        CONNECTED, DISCONNECTED, RECONNECTING, RECONNECTING_FAILED
    }

    public XMPPConnectionEvent(XMPPConnection source, XMPPConnectionEvent.Type event) {
        super(source, event);
    }
}
