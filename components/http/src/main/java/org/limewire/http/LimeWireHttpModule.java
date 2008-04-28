package org.limewire.http;

import org.limewire.http.httpclient.LimeWireHttpClientModule;
import org.limewire.inject.AbstractModule;

/**
 * <code>Guice</code> module to provide bindings for <code>http</code> component classes.
 */
public class LimeWireHttpModule extends AbstractModule {
    
    protected void configure() {
        binder().install(new LimeWireHttpClientModule());
    }
}
