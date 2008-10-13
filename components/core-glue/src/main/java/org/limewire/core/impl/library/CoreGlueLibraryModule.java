package org.limewire.core.impl.library;

import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.inject.AbstractModule;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.TypeLiteral;

public class CoreGlueLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LibraryManager.class).to(LibraryManagerImpl.class);
        bind(RemoteLibraryManager.class).to(RemoteLibraryManagerImpl.class);
        bind(ShareListManager.class).to(LibraryManagerImpl.class);
        bind(PresenceLibraryBrowser.class);
        bind(FriendSearcher.class);
        
        EventMulticaster<FriendShareListEvent> friendShareListMulticaster = new EventMulticasterImpl<FriendShareListEvent>(); 
        bind(new TypeLiteral<EventListener<FriendShareListEvent>>(){}).toInstance(friendShareListMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendShareListEvent>>(){}).toInstance(friendShareListMulticaster);
    }

}
