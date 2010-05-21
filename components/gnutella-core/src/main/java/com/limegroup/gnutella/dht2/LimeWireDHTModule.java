package com.limegroup.gnutella.dht2;

import org.limewire.mojito2.util.HostFilter;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.dht.db.LimeWireDHTDBModule;

public class LimeWireDHTModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireDHTDBModule());
        
        bind(HostFilter.class).to(DefaultHostFilter.class);
        
        bind(InactiveController.class).toInstance(InactiveController.CONTROLLER);
        bind(ActiveController.class);
        bind(PassiveController.class);
        bind(PassiveLeafController.class);
        
        bind(DHTManager.class).to(DHTManagerImpl.class);
    }
}
