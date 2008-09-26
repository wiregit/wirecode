package org.limewire.core.impl.library;

import org.limewire.core.api.library.FriendRemoteLibraryEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.collection.AutoCompleteDictionary;
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
        bind(AutoCompleteDictionary.class).annotatedWith(Names.named("friendLibraries")).to(MockFriendLibraryAutoCompleter.class);

        EventMulticaster<FriendRemoteLibraryEvent> buddyMulticaster = new EventMulticasterImpl<FriendRemoteLibraryEvent>(); 
        bind(new TypeLiteral<EventListener<FriendRemoteLibraryEvent>>(){}).toInstance(buddyMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendRemoteLibraryEvent>>(){}).toInstance(buddyMulticaster);
    }

}
