package org.limewire.activation.impl;

import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.serial.ActivationSerializerModule;

import com.google.inject.AbstractModule;

public class LimeWireActivationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActivationManager.class).to(ActivationManagerImpl.class);
        bind(ActivationCommunicator.class).to(ActivationCommunicatorImpl.class);
        bind(ActivationResponseFactory.class).to(ActivationResponseFactoryImpl.class);
        bind(ActivationModel.class).to(ActivationModelImpl.class);
        bind(ActivationItemFactory.class).to(ActivationItemFactoryImpl.class);
        install(new ActivationSerializerModule());
    }
}
