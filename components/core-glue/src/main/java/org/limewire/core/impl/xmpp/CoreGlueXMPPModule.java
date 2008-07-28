package org.limewire.core.impl.xmpp;

import com.google.inject.TypeLiteral;
import org.limewire.inject.AbstractModule;
import org.limewire.xmpp.client.service.FileOfferHandler;
import org.limewire.xmpp.client.service.RosterListener;
import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.service.XMPPErrorListener;
import org.limewire.xmpp.client.LimeWireXMPPModule;

import java.util.List;

public class CoreGlueXMPPModule extends AbstractModule {
    protected void configure() {
        binder().install(new LimeWireXMPPModule());
        bind(new TypeLiteral<List<XMPPConnectionConfiguration>>(){}).to(XMPPConfigurationListProvider.class);
        bind(RosterListener.class).to(RosterListenerImpl.class);
        bind(XMPPErrorListener.class).to(XMPPErrorListenerImpl.class);
        bind(FileOfferHandler.class).to(FileOfferHandlerImpl.class);
        bind(new TypeLiteral<List<XMPPSettings.XMPPServerConfiguration>>(){}).toProvider(XMPPSettings.XMPPServerConfigs.class);
    }
}
