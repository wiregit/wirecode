package org.limewire.ui.swing.friends;

import com.google.inject.AbstractModule;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class LimeWireUiFriendsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(IconLibrary.class).to(IconLibraryImpl.class);
    }
}
