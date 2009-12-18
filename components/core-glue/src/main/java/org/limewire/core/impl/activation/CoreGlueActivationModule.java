package org.limewire.core.impl.activation;

import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.impl.ActivationManagerImpl;

import com.google.inject.AbstractModule;

public class CoreGlueActivationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActivationManager.class).to(ActivationManagerImpl.class);
    }
}
