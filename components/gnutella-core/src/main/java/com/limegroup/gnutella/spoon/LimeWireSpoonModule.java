package com.limegroup.gnutella.spoon;

import com.google.inject.AbstractModule;

public class LimeWireSpoonModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SpoonSearcher.class).to(SpoonSearcherImpl.class);
    }
}
