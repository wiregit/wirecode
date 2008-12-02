package org.limewire.core.impl.xmpp;

import org.limewire.core.api.xmpp.RemoteFileItemFactory;
import org.limewire.core.api.xmpp.XMPPResourceFactory;
import org.limewire.inject.AbstractModule;
import org.limewire.xmpp.api.client.PasswordManager;
import org.limewire.xmpp.client.LimeWireXMPPModule;
import org.limewire.xmpp.client.impl.PasswordManagerImpl;

public class CoreGlueXMPPModule extends AbstractModule {
    protected void configure() {
        binder().install(new LimeWireXMPPModule());
        bind(XmppPresenceLibraryAdder.class);
        bind(FriendShareListRefresher.FriendShareListEventImpl.class);
        bind(RemoteFileItemFactory.class).to(RemoteFileItemFactoryImpl.class);
        bind(CoreGlueXMPPService.class);
        bind(PasswordManager.class).to(PasswordManagerImpl.class);
        bind(XMPPResourceFactory.class).to(XMPPResourceFactoryImpl.class);
        
        bind(XMPPFirewalledAddressConnector.class).asEagerSingleton();
    }
}
