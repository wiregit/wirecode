package org.limewire.core.impl.xmpp;

import java.util.List;

import org.limewire.xmpp.client.service.FileOfferHandler;
import org.limewire.xmpp.client.service.XMPPConnection;
import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.service.XMPPErrorListener;
import org.limewire.xmpp.client.service.XMPPService;

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
