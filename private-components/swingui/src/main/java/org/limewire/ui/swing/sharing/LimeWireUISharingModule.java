package org.limewire.ui.swing.sharing;


import com.google.inject.AbstractModule;

public class LimeWireUISharingModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendSharingDisplay.class).to(FriendSharingDisplayImpl.class);
    }
}
