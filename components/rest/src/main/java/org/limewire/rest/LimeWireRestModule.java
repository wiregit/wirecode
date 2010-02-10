package org.limewire.rest;

import com.google.inject.AbstractModule;

/**
 * Guice module to configure the REST API components.
 */
public class LimeWireRestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RestRequestHandlerFactory.class).to(RestRequestHandlerFactoryImpl.class);
    }

}
