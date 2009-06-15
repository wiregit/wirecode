package org.limewire.core.api.friend.impl;

import java.util.Collection;
import java.util.Set;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendManager;
import org.limewire.core.api.friend.MutableFriendManager;
import org.limewire.core.api.friend.client.FriendConnectionFactory;
import org.limewire.core.api.friend.client.FriendConnectionFactoryRegistry;
import org.limewire.core.api.friend.feature.features.AuthTokenRegistry;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

public class LimeWireFriendImplModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(FriendConnectionFactoryRegistry.class).to(FriendConnectionFactoryRegistryImpl.class);
        bind(FriendConnectionFactory.class).to(FriendConnectionFactoryRegistryImpl.class);

        bind(FriendManager.class).to(MutableFriendManagerImpl.class);
        bind(MutableFriendManager.class).to(MutableFriendManagerImpl.class);

        bind(AuthTokenRegistry.class).to(DefaultFriendAuthenticator.class).asEagerSingleton();
    }
    
    @Provides @Named("known") Collection<Friend> knownFriendsList(MutableFriendManagerImpl friendManager) {
        return friendManager.getKnownFriends();
    }
    
    @Provides @Named("available") Collection<Friend> availableFriendsList(MutableFriendManagerImpl friendManager) {
        return friendManager.getAvailableFriends();
    }
    
    @Provides @Named("availableFriendIds") Set<String> availableFriendIds(MutableFriendManagerImpl friendManager) {
        return friendManager.getAvailableFriendIds();
    }
}
