package org.limewire.core.api.friend.impl;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executor;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendManager;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.MutableFriendManager;
import org.limewire.core.api.friend.address.FriendAddressRegistry;
import org.limewire.core.api.friend.address.FriendAddressResolver;
import org.limewire.core.api.friend.address.FriendAddressSerializer;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.client.FriendConnectionFactory;
import org.limewire.core.api.friend.client.FriendConnectionFactoryRegistry;
import org.limewire.core.api.friend.client.LibraryChangedEvent;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.FeatureRegistryImpl;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.core.api.friend.feature.features.AddressDispatcher;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.core.api.friend.feature.features.AuthTokenDispatcher;
import org.limewire.core.api.friend.feature.features.LibraryChangedDispatcher;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.io.Address;
import org.limewire.listener.AsynchronousMulticaster;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.CachingEventMulticasterImpl;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.LogFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class LimeWireFriendModule extends AbstractModule {

    @Override
    protected void configure() {
        EventMulticaster<FriendEvent> knownMulticaster = new EventMulticasterImpl<FriendEvent>(FriendEvent.class);
        EventMulticaster<FriendEvent> availMulticaster = new EventMulticasterImpl<FriendEvent>();
        EventMulticaster<FriendPresenceEvent> presenceMulticaster = new EventMulticasterImpl<FriendPresenceEvent>();
        EventMulticaster<FeatureEvent> featureMulticaster = new EventMulticasterImpl<FeatureEvent>();
        
        bind(new TypeLiteral<ListenerSupport<FriendEvent>>(){}).annotatedWith(Names.named("known")).toInstance(knownMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendEvent>>(){}).annotatedWith(Names.named("known")).toInstance(knownMulticaster);
        
        bind(new TypeLiteral<ListenerSupport<FriendEvent>>(){}).annotatedWith(Names.named("available")).toInstance(availMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendEvent>>(){}).annotatedWith(Names.named("available")).toInstance(availMulticaster);
        
        bind(new TypeLiteral<ListenerSupport<FriendPresenceEvent>>(){}).toInstance(presenceMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendPresenceEvent>>(){}).toInstance(presenceMulticaster);

        bind(new TypeLiteral<ListenerSupport<FeatureEvent>>(){}).toInstance(featureMulticaster);
        bind(new TypeLiteral<EventMulticaster<FeatureEvent>>(){}).toInstance(featureMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FeatureEvent>>(){}).toInstance(featureMulticaster);
        
        bind(FeatureRegistry.class).to(FeatureRegistryImpl.class);

        bind(FriendConnectionFactoryRegistry.class).to(FriendConnectionFactoryRegistryImpl.class);
        bind(FriendConnectionFactory.class).to(FriendConnectionFactoryRegistryImpl.class);
        
        bind(new TypeLiteral<FeatureTransport.Handler<Address>>(){}).to(AddressDispatcher.class);
        bind(new TypeLiteral<FeatureTransport.Handler<AuthToken>>(){}).to(AuthTokenDispatcher.class);
        bind(new TypeLiteral<FeatureTransport.Handler<LibraryChangedNotifier>>(){}).to(LibraryChangedDispatcher.class);
        
        bind(FriendManager.class).to(MutableFriendManagerImpl.class);
        bind(MutableFriendManager.class).to(MutableFriendManagerImpl.class);
        
        Executor executor = ExecutorsHelper.newProcessingQueue("FriendConnectionEventThread");
        
        AsynchronousMulticaster<FriendConnectionEvent> asyncConnectionMulticaster =
            new AsynchronousMulticaster<FriendConnectionEvent>(executor, LogFactory.getLog(FriendConnectionEvent.class));
        CachingEventMulticasterImpl<FriendConnectionEvent> connectionMulticaster =
            new CachingEventMulticasterImpl<FriendConnectionEvent>(BroadcastPolicy.IF_NOT_EQUALS, asyncConnectionMulticaster, asyncConnectionMulticaster.getListenerContext());
        bind(new TypeLiteral<EventBean<FriendConnectionEvent>>(){}).toInstance(connectionMulticaster);
        bind(new TypeLiteral<EventMulticaster<FriendConnectionEvent>>(){}).toInstance(connectionMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendConnectionEvent>>(){}).toInstance(connectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendConnectionEvent>>(){}).toInstance(connectionMulticaster);
        
        EventMulticaster<LibraryChangedEvent> libraryChangedMulticaster = new EventMulticasterImpl<LibraryChangedEvent>();
        bind(new TypeLiteral<EventBroadcaster<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);
        bind(new TypeLiteral<ListenerSupport<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);
        
        // bind egearly, so it registers itself with SocketsManager
        bind(FriendAddressResolver.class).asEagerSingleton();
        // dito
        bind(FriendAddressSerializer.class).asEagerSingleton();
        
        bind(DefaultFriendAuthenticator.class).asEagerSingleton();
        
        bind(FriendAddressRegistry.class);
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
