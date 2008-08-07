package org.limewire.core.impl.xmpp;

import java.util.List;

import org.limewire.xmpp.api.client.FileOfferHandler;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPService;

class MockXmppService implements XMPPService {

    @Override
    public void addConnectionConfiguration(XMPPConnectionConfiguration configuration) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<XMPPConnection> getConnections() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void register(XMPPErrorListener errorListener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void register(FileOfferHandler offerHandler) {
        // TODO Auto-generated method stub
        
    }

}
