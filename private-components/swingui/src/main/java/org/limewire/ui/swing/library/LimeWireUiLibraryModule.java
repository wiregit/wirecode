package org.limewire.ui.swing.library;

import com.google.inject.AbstractModule;

public class LimeWireUiLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendLibraryPanel.class).to(FriendLibraryPanelImpl.class);
        bind(MyLibraryPanel.class).to(MyLibraryPanelImpl.class);

    }

}
