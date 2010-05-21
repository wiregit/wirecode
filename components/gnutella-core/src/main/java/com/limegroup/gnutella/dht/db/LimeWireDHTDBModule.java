package com.limegroup.gnutella.dht.db;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class LimeWireDHTDBModule extends AbstractModule {

    @Override
    protected void configure() {
        
        bind(PublisherQueue.class);
        bind(AltLocPublisher.class);
        bind(PushProxiesPublisher.class);
        bind(PushEndpointService.class).to(PushEndpointManager.class);
        
        bind(AltLocFinder.class).to(AltLocFinderImpl.class);
        
        bind(PushEndpointService.class).annotatedWith(
                Names.named("pushEndpointManager")).to(PushEndpointManager.class);
        
        bind(PushEndpointService.class).annotatedWith(
                Names.named("dhtPushEndpointFinder")).to(DHTPushEndpointFinder.class);
    }
}
