package org.limewire.ui.swing.xmpp;

import org.limewire.inject.AbstractModule;
import org.limewire.xmpp.api.client.FileOfferHandler;
import org.limewire.xmpp.api.client.RosterListener;
import org.limewire.xmpp.api.client.XMPPErrorListener;

import com.google.inject.Scopes;

public class LimeWireUiXMPPModule extends AbstractModule {
    protected void configure() {
        bind(RosterListener.class).to(RosterListenerImpl.class).in(Scopes.SINGLETON);
        bind(XMPPErrorListener.class).to(XMPPErrorListenerImpl.class).in(Scopes.SINGLETON);
        bind(FileOfferHandler.class).to(FileOfferHandlerImpl.class).in(Scopes.SINGLETON);
    }
}
