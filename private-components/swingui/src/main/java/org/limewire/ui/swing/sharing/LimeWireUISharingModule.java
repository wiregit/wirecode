package org.limewire.ui.swing.sharing;


import org.limewire.ui.swing.sharing.fancy.LimeWireUiSharingFancyModule;

import com.google.inject.AbstractModule;

public class LimeWireUISharingModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendSharingDisplay.class).to(FriendSharingDisplayImpl.class);
        
        install(new LimeWireUiSharingFancyModule());
    }
}
