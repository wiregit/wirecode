package com.limegroup.gnutella.search;

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
        return 150;
    }
}
