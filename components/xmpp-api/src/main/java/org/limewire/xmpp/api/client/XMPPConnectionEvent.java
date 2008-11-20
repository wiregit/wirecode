package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultEvent;

public class XMPPConnectionEvent extends DefaultEvent<XMPPConnection, XMPPConnection.ConnectionEvent> {

    public XMPPConnectionEvent(XMPPConnection source, XMPPConnection.ConnectionEvent event) {
        super(source, event);
    }
}
