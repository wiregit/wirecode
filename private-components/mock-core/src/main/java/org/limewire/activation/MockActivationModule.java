package org.limewire.activation;

import org.limewire.activation.api.ActivationManager;

import com.google.inject.AbstractModule;

public class MockActivationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActivationManager.class).to(MockActivationManager.class);
    }
}
