package org.limewire.xmpp.api.client;

import com.google.inject.Inject;

public interface XMPPErrorListener {
    @Inject 
    public void register(XMPPService xmppService);
    public void error(XMPPException exception);
}
