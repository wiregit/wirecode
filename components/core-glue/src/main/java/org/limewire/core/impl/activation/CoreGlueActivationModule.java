package org.limewire.core.impl.activation;

import org.limewire.activation.impl.ActivationModule;

import com.google.inject.AbstractModule;

public class CoreGlueActivationModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new ActivationModule());
    }
}
