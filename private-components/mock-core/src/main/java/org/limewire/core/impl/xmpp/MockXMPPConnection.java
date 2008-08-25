package org.limewire.core.impl.xmpp;

import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.Presence.Mode;

public class MockXMPPConnection implements XMPPConnection {
    private XMPPConnectionConfiguration config;
    
    public MockXMPPConnection(XMPPConnectionConfiguration config) {
        this.config = config;
    }

    @Override
    public XMPPConnectionConfiguration getConfiguration() {
        return config;
    }

    @Override
    public boolean isLoggedIn() {
        return true;
    }

    @Override
    public void login() throws XMPPException {
        
    }

    @Override
    public void logout() {
        
    }

    @Override
    public void setMode(Mode mode) {
        
    }
}
