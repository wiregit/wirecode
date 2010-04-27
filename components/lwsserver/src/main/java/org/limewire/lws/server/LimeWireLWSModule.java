package org.limewire.lws.server;

import com.google.inject.AbstractModule;

public class LimeWireLWSModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(LWSDispatcherFactory.class).to(LWSDispatcherFactoryImpl.class);
    }

}
