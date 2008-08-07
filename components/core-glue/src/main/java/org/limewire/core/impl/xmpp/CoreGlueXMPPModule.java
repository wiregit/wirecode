package org.limewire.core.impl.xmpp;

import java.util.List;

import org.limewire.inject.AbstractModule;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.LimeWireXMPPModule;

import com.google.inject.TypeLiteral;

public class CoreGlueXMPPModule extends AbstractModule {
    protected void configure() {
        binder().install(new LimeWireXMPPModule());
        bind(new TypeLiteral<List<XMPPConnectionConfiguration>>(){}).to(XMPPConfigurationListProvider.class);
        bind(new TypeLiteral<List<XMPPSettings.XMPPServerConfiguration>>(){}).toProvider(XMPPSettings.XMPPServerConfigs.class);
    }
}
