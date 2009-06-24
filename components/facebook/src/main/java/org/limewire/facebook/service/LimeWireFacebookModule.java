package org.limewire.facebook.service;

import org.limewire.facebook.service.livemessage.LimeWireFacebookLiveMessageModule;
import org.limewire.facebook.service.settings.FacebookAPIKey;
import org.limewire.inject.AbstractModule;

import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireFacebookModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new LimeWireFacebookLiveMessageModule());
        bind(String.class).annotatedWith(FacebookAPIKey.class).toInstance("9d8c6048e08ffe11d94e9cf3880f6757");
        bind(FacebookFriendService.class);
        bind(ChatListenerFactory.class).toProvider(FactoryProvider.newFactory(ChatListenerFactory.class, ChatListener.class));
        bind(PresenceListenerFactory.class).toProvider(FactoryProvider.newFactory(PresenceListenerFactory.class, PresenceListener.class));
        bind(FacebookFriendConnectionFactory.class).toProvider(FactoryProvider.newFactory(FacebookFriendConnectionFactory.class, FacebookFriendConnection.class));
        bind(FacebookFriendFactory.class).toProvider(FactoryProvider.newFactory(FacebookFriendFactory.class, FacebookFriend.class));
    }
}