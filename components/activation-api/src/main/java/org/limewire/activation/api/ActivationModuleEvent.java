package org.limewire.activation.api;

import org.limewire.listener.DefaultDataEvent;

public class ActivationModuleEvent extends DefaultDataEvent<Integer> {

    private final boolean isActive;
    
    public ActivationModuleEvent(int data, boolean isActive) {
        super(data);
        
        this.isActive = isActive;
    }
    
    public boolean isActive() {
        return isActive;
    }
}
