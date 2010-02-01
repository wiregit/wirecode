package org.limewire.core.impl.rest;

import org.limewire.core.impl.rest.handler.RestRequestHandlerFactory;
import org.limewire.core.impl.rest.handler.RestRequestHandlerFactoryImpl;
import org.limewire.inject.AbstractModule;

/**
 * Guice module to configure the REST API for the live core.
 */
public class CoreGlueRestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(CoreGlueRestService.class);
        bind(RestRequestHandlerFactory.class).to(RestRequestHandlerFactoryImpl.class);
    }

}
