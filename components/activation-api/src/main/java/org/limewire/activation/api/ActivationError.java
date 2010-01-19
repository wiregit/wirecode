package org.limewire.activation.api;

/**
 * Error States the ActivationManager can exists in.
 */
public enum ActivationError {

    /** No error currently exists. */
    NO_ERROR,
    
    /** No Key exists. */
    NO_KEY,
    
    /** The current Key is invalid. */
    INVALID_KEY,
    
    /** The current Key is expired. */
    EXPIRED_KEY,
    
    /** The current Key has been blocked by the server.  */
    BLOCKED_KEY,
    
    /** There was a problem contacting the server. */
    COMMUNICATION_ERROR,
}
