package com.limegroup.gnutella.version;

import org.limewire.activation.api.ActivationManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;

@Singleton
public class UpdateCollectionFactoryImpl implements UpdateCollectionFactory {

    private final ApplicationServices applicationServices;
    private final ActivationManager activationManager;

    @Inject
    public UpdateCollectionFactoryImpl(ApplicationServices applicationServices, ActivationManager activationManager) {
        this.applicationServices = applicationServices;
        this.activationManager = activationManager;
    }
    
    public UpdateCollection createUpdateCollection(String xml) {
        return new UpdateCollectionImpl(xml, applicationServices, activationManager);
    }

}
