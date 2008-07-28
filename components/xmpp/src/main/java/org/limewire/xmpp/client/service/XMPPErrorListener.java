package org.limewire.xmpp.client.service;

import org.limewire.xmpp.client.impl.XMPPException;

import com.google.inject.Inject;

public interface XMPPErrorListener {
    @Inject 
    public void register(XMPPService xmppService);
    public void error(XMPPException exception);
}
