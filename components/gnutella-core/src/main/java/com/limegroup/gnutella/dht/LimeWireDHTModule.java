package com.limegroup.gnutella.dht;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocFinderImpl;
import com.limegroup.gnutella.dht.db.PushEndpointManager;
import com.limegroup.gnutella.dht.db.PushEndpointService;

public class LimeWireDHTModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AltLocFinder.class).to(AltLocFinderImpl.class);
        bind(PushEndpointService.class).annotatedWith(Names.named("pushEndpointManager")).to(PushEndpointManager.class);
    }

}
