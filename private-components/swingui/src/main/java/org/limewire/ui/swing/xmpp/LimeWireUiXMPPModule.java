package org.limewire.ui.swing.xmpp;

import org.limewire.inject.AbstractModule;
import org.limewire.xmpp.client.service.FileOfferHandler;
import org.limewire.xmpp.client.service.RosterListener;
import org.limewire.xmpp.client.service.XMPPErrorListener;

public class LimeWireUiXMPPModule extends AbstractModule {
    protected void configure() {
        bind(RosterListener.class).to(RosterListenerImpl.class);
        bind(XMPPErrorListener.class).to(XMPPErrorListenerImpl.class);
        bind(FileOfferHandler.class).to(FileOfferHandlerImpl.class);
    }
}
