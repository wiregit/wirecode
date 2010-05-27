package com.limegroup.gnutella.dht;

import org.limewire.mojito.util.HostFilter;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.dht.db.LimeWireDHTDBModule;

public class LimeWireDHTModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireDHTDBModule());
        
        bind(HostFilter.class).to(DefaultHostFilter.class);
        bind(DHTManager.class).to(DHTManagerImpl.class);
    }
}
