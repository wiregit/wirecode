package org.limewire.core.impl.friend;

import org.limewire.core.api.xmpp.RemoteFileItemFactory;

import com.google.inject.AbstractModule;

public class CoreGlueFriendModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FriendFirewalledAddressConnector.class).asEagerSingleton();
        bind(FriendRemoteFileDescCreator.class).asEagerSingleton();
        bind(RemoteFileItemFactory.class).to(RemoteFileItemFactoryImpl.class);
    }

}
