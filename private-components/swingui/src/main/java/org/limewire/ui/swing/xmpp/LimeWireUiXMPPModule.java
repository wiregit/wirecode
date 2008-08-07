package org.limewire.ui.swing.xmpp;

import org.limewire.inject.AbstractModule;
import org.limewire.xmpp.api.client.FileOfferHandler;
import org.limewire.xmpp.api.client.RosterListener;
import org.limewire.xmpp.api.client.XMPPErrorListener;

public class LimeWireUiXMPPModule extends AbstractModule {
    protected void configure() {
        bind(RosterListener.class).to(RosterListenerImpl.class);
        bind(XMPPErrorListener.class).to(XMPPErrorListenerImpl.class);
        bind(FileOfferHandler.class).to(FileOfferHandlerImpl.class);
    }
}
