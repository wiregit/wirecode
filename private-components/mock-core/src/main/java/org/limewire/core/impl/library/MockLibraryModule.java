package org.limewire.core.impl.library;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.collection.AutoCompleteDictionary;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;


public class MockLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LibraryManager.class).to(MockLibraryManager.class);
        bind(AutoCompleteDictionary.class).annotatedWith(Names.named("friendLibraries")).to(MockFriendLibraryAutoCompleter.class);
    }

}
