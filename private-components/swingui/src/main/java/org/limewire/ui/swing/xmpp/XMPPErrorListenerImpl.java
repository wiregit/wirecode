package org.limewire.ui.swing.xmpp;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import org.limewire.xmpp.client.impl.XMPPException;
import org.limewire.xmpp.client.service.XMPPErrorListener;
import org.limewire.xmpp.client.service.XMPPService;

@Singleton
class XMPPErrorListenerImpl implements XMPPErrorListener{
    
    @Inject
    public void register(XMPPService xmppService) {
        xmppService.register(this);
    }
    public void error(XMPPException exception) {
        // TODO update UI
    }
}
