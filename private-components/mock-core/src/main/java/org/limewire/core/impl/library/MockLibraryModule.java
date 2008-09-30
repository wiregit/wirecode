package org.limewire.core.impl.library;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.library.FriendRemoteLibraryEvent;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;


public class MockLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LibraryManager.class).to(MockLibraryManager.class);
        bind(RemoteLibraryManager.class).to(MockLibraryManager.class);
        bind(ShareListManager.class).to(MockLibraryManager.class);
        bind(AutoCompleteDictionary.class).annotatedWith(Names.named("friendLibraries")).to(MockFriendLibraryAutoCompleter.class);

        EventMulticaster<FriendRemoteLibraryEvent> friendLibraryMulticaster = new EventMulticasterImpl<FriendRemoteLibraryEvent>(); 
        bind(new TypeLiteral<EventListener<FriendRemoteLibraryEvent>>(){}).toInstance(friendLibraryMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendRemoteLibraryEvent>>(){}).toInstance(friendLibraryMulticaster);
        
        EventMulticaster<FriendShareListEvent> friendShareListMulticaster = new EventMulticasterImpl<FriendShareListEvent>(); 
        bind(new TypeLiteral<EventListener<FriendShareListEvent>>(){}).toInstance(friendShareListMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendShareListEvent>>(){}).toInstance(friendShareListMulticaster);
    }

}
