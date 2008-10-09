package org.limewire.ui.swing.friends;

import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class XMPPConnectionListenerImpl implements RegisteringEventListener<XMPPConnectionEvent> {

    public void handleEvent(XMPPConnectionEvent event) {
        new XMPPConnectionEstablishedEvent(event.getSource()).publish();
    }

    @Inject
    public void register(ListenerSupport<XMPPConnectionEvent> listenerSupport) {
        listenerSupport.addListener(this);
    }
}
