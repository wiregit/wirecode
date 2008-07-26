package org.limewire.xmpp.client.service;

import org.limewire.xmpp.client.impl.XMPPException;

public interface XMPPErrorListener {
    public void error(XMPPException exception);
}
