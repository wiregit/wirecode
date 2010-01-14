package com.limegroup.gnutella.search;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class QuerySettings {

    @SuppressWarnings("unused")
    private final ActivationManager activationManager; 
    
    @Inject
    public QuerySettings(ActivationManager activationManager) {
        this.activationManager = activationManager;
    }
    
    public int getUltrapeerResults() {
        //TODO: this needs to get changed in installer
//        return 150;
        return activationManager.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE) ? 250 : 150;
    }
}
