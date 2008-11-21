package org.limewire.ui.swing.statusbar;

import org.limewire.ui.swing.mainframe.UnseenMessageListener;

import com.google.inject.AbstractModule;

public class LimeWireUiStatusbarModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(UnseenMessageListener.class).to(FriendStatusPanel.class);        
    }


}
