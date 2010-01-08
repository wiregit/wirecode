package org.limewire.core.impl.activation;

import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.impl.ActivationManagerImpl;
import org.limewire.activation.impl.ActivationModel;
import org.limewire.activation.impl.ActivationModelImpl;
import org.limewire.activation.serial.ActivationSerializer;
import org.limewire.activation.serial.ActivationSerializerImpl;

import com.google.inject.AbstractModule;

public class CoreGlueActivationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActivationManager.class).to(ActivationManagerImpl.class);
        bind(ActivationModel.class).to(ActivationModelImpl.class);
        bind(ActivationSerializer.class).to(ActivationSerializerImpl.class);
    }
}
