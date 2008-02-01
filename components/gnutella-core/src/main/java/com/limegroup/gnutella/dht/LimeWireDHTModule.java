package com.limegroup.gnutella.dht;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocFinderImpl;

public class LimeWireDHTModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AltLocFinder.class).to(AltLocFinderImpl.class);
    }

}
