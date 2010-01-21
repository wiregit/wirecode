package org.limewire.activation.impl;

import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.serial.ActivationSerializer;
import org.limewire.activation.serial.ActivationSerializerImpl;
import org.limewire.activation.serial.ActivationSerializerSettings;
import org.limewire.activation.serial.ActivationSerializerSettingsImpl;

import com.google.inject.AbstractModule;

public class ActivationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActivationManager.class).to(ActivationManagerImpl.class);
        bind(ActivationCommunicator.class).to(ActivationCommunicatorImpl.class);
        bind(ActivationResponseFactory.class).to(ActivationResponseFactoryImpl.class);
        bind(ActivationModel.class).to(ActivationModelImpl.class);
        bind(ActivationSerializer.class).to(ActivationSerializerImpl.class);
        bind(ActivationItemFactory.class).to(ActivationItemFactoryImpl.class);
        bind(ActivationSerializerSettings.class).to(ActivationSerializerSettingsImpl.class);
    }
}
