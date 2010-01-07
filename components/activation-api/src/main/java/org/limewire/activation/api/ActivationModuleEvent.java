package org.limewire.activation.api;

import org.limewire.listener.DefaultDataEvent;

public class ActivationModuleEvent extends DefaultDataEvent<ActivationID> {

    private final boolean isActive;
    
    public ActivationModuleEvent(ActivationID moduleID, boolean isActive) {
        super(moduleID);
        
        this.isActive = isActive;
    }
    
    public boolean isActive() {
        return isActive;
    }
}
