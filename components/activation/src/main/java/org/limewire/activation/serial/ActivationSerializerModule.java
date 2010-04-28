package org.limewire.activation.serial;

import com.google.inject.AbstractModule;


/**
 * Used to bind classes related to serialization/deserialization
 * of responses from the activation server.
 */
public class ActivationSerializerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActivationSerializer.class).to(ActivationSerializerImpl.class);
        bind(ActivationSerializerSettings.class).to(ActivationSerializerSettingsImpl.class);
    }
}
