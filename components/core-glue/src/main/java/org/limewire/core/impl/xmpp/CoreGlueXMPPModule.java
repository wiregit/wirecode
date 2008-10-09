package org.limewire.core.impl.xmpp;

import java.util.List;
import java.util.Map;

import org.limewire.inject.AbstractModule;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.LimeWireXMPPModule;

import com.google.inject.TypeLiteral;

public class CoreGlueXMPPModule extends AbstractModule {
    protected void configure() {
        binder().install(new LimeWireXMPPModule());
        bind(XmppPresenceLibraryAdder.class);
        bind(new TypeLiteral<List<XMPPConnectionConfiguration>>(){}).to(XMPPConfigurationListProvider.class);
        bind(new TypeLiteral<Map<String, XMPPServerSettings.XMPPServerConfiguration>>(){}).toProvider(XMPPServerSettings.XMPPServerConfigs.class);
        bind(new TypeLiteral<Map<String, XMPPUserSettings.XMPPUserConfiguration>>(){}).toProvider(XMPPUserSettings.XMPPUserConfigs.class);
        bind(FriendShareListRefresher.FriendShareListEventImpl.class);
        bind(FriendShareListRefresher.RosterEventListenerImpl.class);
    }
}
