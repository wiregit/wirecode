package org.limewire.activation.api;

import org.limewire.listener.DefaultDataEvent;

/**
 * An event that gets created and broadcast when the ActivationState of
 * the ActivationManager changes. This reports the new state of the 
 * ActivationManager along with any error that may have occurred to send
 * it to that new state.
 */
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
