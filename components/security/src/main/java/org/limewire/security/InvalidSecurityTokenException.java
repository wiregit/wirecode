package org.limewire.security;

import java.io.IOException;

/**
 * Exception thrown by security tokens when they are constructed with
 * invalid data from, for example from the network. 
 */
public class InvalidSecurityTokenException extends IOException {

    public InvalidSecurityTokenException(String message) {
        super(message);
    }
    
}
