package org.limewire.core.impl.xmpp;

import org.limewire.listener.EventListener;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

class MockXmppService implements XMPPService {

    @Override
    public void login(XMPPConnectionConfiguration configuration) {
    }

    @Override
    public void logout() {
    }
    
    @Override
    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public XMPPConnection getLoggedInConnection() {
        return null;
    }

    @Override
    public void setXmppErrorListener(EventListener<XMPPException> errorListener) {
    }
}
