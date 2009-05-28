package org.limewire.facebook.service;

import org.limewire.facebook.service.livemessage.AddressHandler;
import org.limewire.facebook.service.livemessage.AddressHandlerFactory;
import org.limewire.facebook.service.livemessage.AuthTokenHandler;
import org.limewire.facebook.service.livemessage.AuthTokenHandlerFactory;
import org.limewire.facebook.service.livemessage.ConnectBackRequestHandler;
import org.limewire.facebook.service.livemessage.ConnectBackRequestHandlerFactory;
import org.limewire.facebook.service.livemessage.DiscoInfoHandler;
import org.limewire.facebook.service.livemessage.DiscoInfoHandlerFactory;
import org.limewire.facebook.service.livemessage.LibraryRefreshHandler;
import org.limewire.facebook.service.livemessage.LibraryRefreshHandlerFactory;
import org.limewire.facebook.service.livemessage.LiveMessageHandlerRegistry;
import org.limewire.facebook.service.livemessage.LiveMessageHandlerRegistryImpl;
import org.limewire.facebook.service.livemessage.PresenceHandler;
import org.limewire.facebook.service.livemessage.PresenceHandlerFactory;
import org.limewire.inject.AbstractModule;

import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.name.Names;

public class LimeWireFacebookModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("facebookApiKey")).toInstance("9d8c6048e08ffe11d94e9cf3880f6757");
        bind(FacebookFriendService.class);
        bind(LiveMessageHandlerRegistry.class).to(LiveMessageHandlerRegistryImpl.class);
        bind(ChatListenerFactory.class).toProvider(FactoryProvider.newFactory(ChatListenerFactory.class, ChatListener.class));
        bind(PresenceListenerFactory.class).toProvider(FactoryProvider.newFactory(PresenceListenerFactory.class, PresenceListener.class));
        bind(FacebookFriendConnectionFactory.class).toProvider(FactoryProvider.newFactory(FacebookFriendConnectionFactory.class, FacebookFriendConnection.class));
        bind(AddressHandlerFactory.class).toProvider(FactoryProvider.newFactory(AddressHandlerFactory.class, AddressHandler.class));
        bind(AuthTokenHandlerFactory.class).toProvider(FactoryProvider.newFactory(AuthTokenHandlerFactory.class, AuthTokenHandler.class));
        bind(DiscoInfoHandlerFactory.class).toProvider(FactoryProvider.newFactory(DiscoInfoHandlerFactory.class, DiscoInfoHandler.class));
        bind(PresenceHandlerFactory.class).toProvider(FactoryProvider.newFactory(PresenceHandlerFactory.class, PresenceHandler.class));
        bind(ConnectBackRequestHandlerFactory.class).toProvider(FactoryProvider.newFactory(ConnectBackRequestHandlerFactory.class, ConnectBackRequestHandler.class));
        bind(LibraryRefreshHandlerFactory.class).toProvider(FactoryProvider.newFactory(LibraryRefreshHandlerFactory.class, LibraryRefreshHandler.class));
        bind(FacebookFriendFactory.class).toProvider(FactoryProvider.newFactory(FacebookFriendFactory.class, FacebookFriend.class));
    }
}