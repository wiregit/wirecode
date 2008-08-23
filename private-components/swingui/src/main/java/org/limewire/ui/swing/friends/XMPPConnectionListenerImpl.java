package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.XMPPConnectionListener;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class XMPPConnectionListenerImpl implements XMPPConnectionListener {

    public void connected(String connectionId) {
        new XMPPConnectionEstablishedEvent(connectionId).publish();
    }

    @Inject
    public void register(XMPPService xmppService) {
        xmppService.setConnectionListener(this);
    }
}
