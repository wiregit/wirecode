package org.limewire.core.impl.xmpp;

import java.util.Collections;
import java.util.List;

import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionListener;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPService;

class MockXmppService implements XMPPService {
//    private XMPPConnection connection;

//    @Inject
    @Override
    public void addConnectionConfiguration(XMPPConnectionConfiguration configuration) {
//        connection = new MockXMPPConnection(configuration);
    }

    @Override
    public List<XMPPConnection> getConnections() {
//        return Arrays.asList(connection);
        return Collections.emptyList();
    }

    @Override
    public void setXmppErrorListener(XMPPErrorListener errorListener) {
        // TODO Auto-generated method stub
        
    }
}
