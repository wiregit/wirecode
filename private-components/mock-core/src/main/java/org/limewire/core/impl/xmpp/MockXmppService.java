package org.limewire.core.impl.xmpp;

import java.util.Collections;
import java.util.List;

import org.limewire.xmpp.api.client.FileOfferHandler;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionListener;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPService;

class MockXmppService implements XMPPService {

    @Override
    public void addConnectionConfiguration(XMPPConnectionConfiguration configuration) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<XMPPConnection> getConnections() {
        return Collections.emptyList();
    }

    @Override
    public void setXmppErrorListener(XMPPErrorListener errorListener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setFileOfferHandler(FileOfferHandler offerHandler) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setConnectionListener(XMPPConnectionListener connectionListener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public XMPPConnectionListener getConnectionListener() {
        // TODO Auto-generated method stub
        return null;
    }
}
