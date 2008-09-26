package org.limewire.core.impl.library;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.library.FriendRemoteLibraryEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.inject.AbstractModule;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class CoreGlueLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LibraryManager.class).to(LibraryManagerImpl.class);
        bind(LibraryRosterListener.class);
        bind(AutoCompleteDictionary.class).annotatedWith(Names.named("friendLibraries")).to(FriendLibraryAutoCompleter.class);
        bind(FriendSearcher.class);
        
        EventMulticaster<FriendRemoteLibraryEvent> friendMulticaster = new EventMulticasterImpl<FriendRemoteLibraryEvent>(); 
        bind(new TypeLiteral<EventListener<FriendRemoteLibraryEvent>>(){}).toInstance(friendMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendRemoteLibraryEvent>>(){}).toInstance(friendMulticaster);
    }

}
