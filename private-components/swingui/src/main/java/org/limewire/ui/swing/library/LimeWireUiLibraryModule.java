package org.limewire.ui.swing.library;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableFactoryImpl;
import org.limewire.ui.swing.library.table.menu.LocalFileItemPropertiesFactory;
import org.limewire.ui.swing.library.table.menu.RemoteFileItemPropertiesFactory;
import org.limewire.ui.swing.properties.PropertiesFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendLibraryFactory.class).toProvider(
                FactoryProvider.newFactory(FriendLibraryFactory.class, FriendLibraryPanel.class));
        bind(MyLibraryFactory.class).toProvider(
                FactoryProvider.newFactory(MyLibraryFactory.class, MyLibraryPanel.class));
        bind(LibraryTableFactory.class).to(LibraryTableFactoryImpl.class);
        bind(new TypeLiteral<PropertiesFactory<LocalFileItem>>(){}).to(LocalFileItemPropertiesFactory.class);
        bind(new TypeLiteral<PropertiesFactory<RemoteFileItem>>(){}).to(RemoteFileItemPropertiesFactory.class);
    }
}
