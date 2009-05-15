package org.limewire.facebook.service;

import org.limewire.inject.AbstractModule;

import com.google.inject.assistedinject.FactoryProvider;

public class LimewireFacebookModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(FacebookFriendService.class);
        bind(FacebookFriendConnection.class);
        bind(BuddyListResponseDeserializer.class);
        bind(LiveMessageAddressTransport.class);
        bind(LiveMessageAuthTokenTransport.class);
        bind(LiveMessageHandlerRegistry.class).to(LiveMessageHandlerRegistryImpl.class);
        bind(BuddyListResponseDeserializerFactory.class).toProvider(FactoryProvider.newFactory(BuddyListResponseDeserializerFactory.class, BuddyListResponseDeserializer.class));
        bind(ChatClientFactory.class).toProvider(FactoryProvider.newFactory(ChatClientFactory.class, ChatClient.class));
        //bind(AddressSenderFactory.class).toProvider(FactoryProvider.newFactory(AddressSenderFactory.class, AddressSender.class));
        bind(PresenceListenerFactory.class).toProvider(FactoryProvider.newFactory(PresenceListenerFactory.class, PresenceListener.class));
    }
}
