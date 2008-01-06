package org.limewire.http;

import com.google.inject.AbstractModule;

public class LimeWireHttpModule extends AbstractModule {
    protected void configure() {
        requestStaticInjection(HttpClientManager.class);
    }
}
