package org.limewire.core.impl.activation;

import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.impl.ActivationItemFactory;
import org.limewire.activation.impl.ActivationItemFactoryImpl;
import org.limewire.activation.impl.ActivationManagerImpl;
import org.limewire.activation.impl.ActivationModel;
import org.limewire.activation.impl.ActivationModelImpl;
import org.limewire.activation.impl.ActivationCommunicatorImpl;
import org.limewire.activation.impl.ActivationCommunicator;
import org.limewire.activation.impl.ActivationResponseFactory;
import org.limewire.activation.impl.ActivationResponseFactoryImpl;
import org.limewire.activation.serial.ActivationSerializer;
import org.limewire.activation.serial.ActivationSerializerImpl;
import org.limewire.activation.serial.ActivationSerializerSettings;
import org.limewire.activation.serial.ActivationSerializerSettingsImpl;

import com.google.inject.AbstractModule;

public class CoreGlueActivationModule extends AbstractModule {

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
