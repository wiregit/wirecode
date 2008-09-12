package org.limewire.ui.swing.sharing;


import com.google.inject.AbstractModule;

public class LimeWireUISharingModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(BuddySharingDisplay.class).to(BuddySharingDisplayImpl.class);
    }
}
