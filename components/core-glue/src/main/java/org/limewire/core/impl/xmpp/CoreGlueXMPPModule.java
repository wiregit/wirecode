package org.limewire.core.impl.xmpp;

import java.util.List;
import java.util.Map;

import org.limewire.inject.AbstractModule;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.LimeWireXMPPModule;
import org.limewire.security.MACCalculator;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.concurrent.AbstractLazySingletonProvider;

import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class CoreGlueXMPPModule extends AbstractModule {
    protected void configure() {
        binder().install(new LimeWireXMPPModule());
        bind(XmppPresenceLibraryAdder.class);
        bind(new TypeLiteral<List<XMPPConnectionConfiguration>>(){}).to(XMPPConfigurationListProvider.class);
        bind(new TypeLiteral<Map<String, XMPPServerSettings.XMPPServerConfiguration>>(){}).toProvider(XMPPServerSettings.XMPPServerConfigs.class);
        bind(new TypeLiteral<Map<String, XMPPUserSettings.XMPPUserConfiguration>>(){}).toProvider(XMPPUserSettings.XMPPUserConfigs.class);
        bind(FriendShareListRefresher.FriendShareListEventImpl.class);
        bind(FriendShareListRefresher.RosterEventListenerImpl.class);
        bind(MACCalculator.class).annotatedWith(Names.named("xmppMACCalculator")).toProvider(XMPPSessionMACCalculatorProvider.class);
        bind(XMPPAuthenticator.class);
        bind(CoreGlueXMPPService.class);
    }
    
    private static class XMPPSessionMACCalculatorProvider extends AbstractLazySingletonProvider<MACCalculator> {
        @Override
        protected MACCalculator createObject() {
            return MACCalculatorRepositoryManager.createDefaultCalculatorFactory().createMACCalculator();
        }
    }
}
