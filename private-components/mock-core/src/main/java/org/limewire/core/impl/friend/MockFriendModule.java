package org.limewire.core.impl.friend;

import org.limewire.core.api.friend.FileMetaDataConverter;
import org.limewire.friend.api.FriendEvent;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.api.LibraryChangedEvent;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class MockFriendModule extends AbstractModule {
    
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

        EventMulticaster<LibraryChangedEvent> libraryChangedMulticaster = new EventMulticasterImpl<LibraryChangedEvent>();
        bind(new TypeLiteral<EventBroadcaster<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);
        bind(new TypeLiteral<ListenerSupport<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);
        
        bind(FileMetaDataConverter.class).to(MockFileMetaDataConverter.class);
    }
}
