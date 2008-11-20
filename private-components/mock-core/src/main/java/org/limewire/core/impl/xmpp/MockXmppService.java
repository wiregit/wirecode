package org.limewire.core.impl.xmpp;

import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPService;

class MockXmppService implements XMPPService {

    @Override
    public void login(XMPPConnectionConfiguration configuration) {
    }

    @Override
    public void logout() {
    }

    @Override
    public XMPPConnection getLoggedInConnection() {
        return null;
    }

    @Override
    public void setXmppErrorListener(XMPPErrorListener errorListener) {
    }
}
