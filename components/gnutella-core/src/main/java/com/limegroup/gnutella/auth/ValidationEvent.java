package com.limegroup.gnutella.auth;

import org.limewire.io.URNImpl;

public class ValidationEvent {
    
    public enum Type { VALID, INVALID, UNKNOWN; }
    
    private final URNImpl urn;
    private final Type type;
    
    public ValidationEvent(Type type, URNImpl urn) {
        this.urn = urn;
        this.type = type;
    }
    
    public URNImpl getUrn() {
        return urn;
    }
    
    public Type getType() {
        return type;
    }

}
