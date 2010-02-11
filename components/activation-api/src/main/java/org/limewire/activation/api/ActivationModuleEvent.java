package org.limewire.activation.api;

import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.listener.DefaultDataEvent;

/**
 * An event that gets created and broadcast anytime a given module changes
 * status. 
 */
public class ActivationModuleEvent extends DefaultDataEvent<ActivationID> {

    private final Status status;
    
    public ActivationModuleEvent(ActivationID moduleID, Status status) {
        super(moduleID);
        
        this.status = status;
    }
    
    public Status getStatus() {
        return status;
    }
}
