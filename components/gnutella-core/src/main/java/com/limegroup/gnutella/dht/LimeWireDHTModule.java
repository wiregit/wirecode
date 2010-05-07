package com.limegroup.gnutella.dht;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.dht.db.LimeWireDHTDBModule;
import com.limegroup.gnutella.dht2.DHTManager;

public class LimeWireDHTModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireDHTDBModule());
        
        bind(DHTManager.class);
    }

}
