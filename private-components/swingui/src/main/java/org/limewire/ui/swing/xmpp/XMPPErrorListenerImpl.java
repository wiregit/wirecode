package org.limewire.ui.swing.xmpp;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

@Singleton
class XMPPErrorListenerImpl implements XMPPErrorListener{
    
    @Inject
    public void register(XMPPService xmppService) {
        xmppService.setXmppErrorListener(this);
    }
    public void error(XMPPException exception) {
        // TODO update UI
    }
}
