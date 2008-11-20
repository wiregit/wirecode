package org.limewire.core.impl.xmpp;

import org.limewire.core.api.xmpp.RemoteFileItemFactory;
import org.limewire.inject.AbstractModule;
import org.limewire.xmpp.client.LimeWireXMPPModule;

public class CoreGlueXMPPModule extends AbstractModule {
    protected void configure() {
        binder().install(new LimeWireXMPPModule());
        bind(XmppPresenceLibraryAdder.class);
        bind(FriendShareListRefresher.FriendShareListEventImpl.class);
        bind(RemoteFileItemFactory.class).to(RemoteFileItemFactoryImpl.class);
        bind(CoreGlueXMPPService.class);
    }
}
