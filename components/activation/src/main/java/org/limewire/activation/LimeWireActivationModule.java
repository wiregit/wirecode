package org.limewire.activation;

import org.limewire.inject.AbstractModule;

public class LimeWireActivationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActivationKeyParser.class).to(ActivationKeyParserImpl.class);
    }
}

