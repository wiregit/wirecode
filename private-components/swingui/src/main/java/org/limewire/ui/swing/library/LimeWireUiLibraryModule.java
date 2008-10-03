package org.limewire.ui.swing.library;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendLibraryFactory.class).toProvider(
                FactoryProvider.newFactory(FriendLibraryFactory.class, FriendLibraryPanel.class));
        bind(MyLibraryFactory.class).toProvider(
                FactoryProvider.newFactory(MyLibraryFactory.class, MyLibraryPanel.class));
    }

}
