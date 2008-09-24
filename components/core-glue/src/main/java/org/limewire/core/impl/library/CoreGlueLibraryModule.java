package org.limewire.core.impl.library;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.inject.AbstractModule;

import com.google.inject.name.Names;

public class CoreGlueLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LibraryManager.class).to(LibraryManagerImpl.class);
        bind(LibraryRosterListener.class);
        bind(AutoCompleteDictionary.class).annotatedWith(Names.named("friendLibraries")).to(FriendLibraryAutoCompleter.class);
        bind(FriendSearcher.class);
    }

}
