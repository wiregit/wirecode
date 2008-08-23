package org.limewire.xmpp.client;

import org.limewire.xmpp.api.client.XMPPConnectionListener;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class XMPPConnectionListenerMock implements XMPPConnectionListener {

    @Override
    public void connected(String connectionId) {
        // TODO Auto-generated method stub
    }

    @Inject
    public void register(XMPPService xmppService) {
        xmppService.setConnectionListener(this);
    }
}
