package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.friends.chat.LimeWireUiFriendsChatModule;
import org.limewire.ui.swing.friends.login.LimeWireUiFriendsLoginModule;

import com.google.inject.AbstractModule;


public class LimeWireUiFriendsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimeWireUiFriendsLoginModule());
        install(new LimeWireUiFriendsChatModule());
        bind(FriendRequestHandlerImpl.class);
    }

}
