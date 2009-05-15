package org.limewire.facebook.service;

import org.limewire.inject.AbstractModule;

import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.name.Names;

public class LimewireFacebookModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("facebookApiKey")).toInstance("");
        bind(FacebookFriendService.class);
        //bind(FacebookFriendConnection.class);
        //bind(BuddyListResponseDeserializer.class);
        //bind(LiveMessageAddressTransport.class);
        //bind(LiveMessageAuthTokenTransport.class);
        bind(LiveMessageHandlerRegistry.class).to(LiveMessageHandlerRegistryImpl.class);
        bind(BuddyListResponseDeserializerFactory.class).toProvider(FactoryProvider.newFactory(BuddyListResponseDeserializerFactory.class, BuddyListResponseDeserializer.class));
        bind(ChatClientFactory.class).toProvider(FactoryProvider.newFactory(ChatClientFactory.class, ChatClient.class));
        //bind(AddressSenderFactory.class).toProvider(FactoryProvider.newFactory(AddressSenderFactory.class, AddressSender.class));
        bind(PresenceListenerFactory.class).toProvider(FactoryProvider.newFactory(PresenceListenerFactory.class, PresenceListener.class));
        bind(FacebookFriendConnectionFactory.class).toProvider(FactoryProvider.newFactory(FacebookFriendConnectionFactory.class, FacebookFriendConnection.class));
        bind(LiveMessageAddressTransportFactory.class).toProvider(FactoryProvider.newFactory(LiveMessageAddressTransportFactory.class, LiveMessageAddressTransport.class));
        bind(LiveMessageAuthTokenTransportFactory.class).toProvider(FactoryProvider.newFactory(LiveMessageAuthTokenTransportFactory.class, LiveMessageAuthTokenTransport.class));
    }
}
