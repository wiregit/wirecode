package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultEvent;

public class XMPPConnectionEvent extends DefaultEvent<String, XMPPConnection.ConnectionEvent> {

    public XMPPConnectionEvent(String source, XMPPConnection.ConnectionEvent event) {
        super(source, event);
    }
}
