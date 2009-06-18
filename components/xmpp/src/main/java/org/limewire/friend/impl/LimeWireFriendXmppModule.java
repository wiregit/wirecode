package org.limewire.friend.impl;


import org.limewire.friend.api.LimeWireFriendModule;

import com.google.inject.AbstractModule;

public class LimeWireFriendXmppModule extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimeWireFriendModule());
        bind(FriendListListeners.class);
    }
}
