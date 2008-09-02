package org.limewire.ui.swing.friends;

import java.util.List;

import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;

class ChatLoginStateImpl implements ChatLoginState {
    private final XMPPService service;
    
    @Inject
    public ChatLoginStateImpl(XMPPService service) {
        this.service = service;
    }

    @Override
    public boolean isLoggedIn() {
        List<XMPPConnection> connections = service.getConnections();
        for(XMPPConnection connection : connections) {
            if(connection.isLoggedIn()) {
                return true;
            }
        }
        return false;
    }
}
