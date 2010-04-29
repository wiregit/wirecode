package org.limewire.core.impl.friend;

import org.limewire.friend.api.LimeWireFriendModule;

import com.google.inject.AbstractModule;

public class CoreGlueFriendModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireFriendModule());
    }
}
