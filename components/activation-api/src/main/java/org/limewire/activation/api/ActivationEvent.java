package org.limewire.activation.api;

import org.limewire.listener.DefaultDataEvent;

public class ActivationEvent extends DefaultDataEvent<ActivationState> {    

    private ActivationError error;
    
    public ActivationEvent(ActivationState data) {
        this(data, ActivationError.NO_ERROR);
    }
    
    public ActivationEvent(ActivationState data, ActivationError error) {
        super(data);
    
        this.error = error;
    }
    
    public ActivationError getError() {
        return error;
    }
}
