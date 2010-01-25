package org.limewire.activation.api;

/**
 * States the ActivationManager can cycle through. These ActivationStates
 * correspond directly to the state of a LicenseKey. This state has nothing
 * to do with the state of a given Module that is associated with a given
 * LicenseKey.
 */
public enum ActivationState {

    /**
     * There is no License Key or the current License Key is 
     * not authorized.
     */
    NOT_AUTHORIZED,
    
    /**
     * A new License Key is currently being authenticated. 
     */
    AUTHORIZING,

    /**
     * The License Key that exists is currently being re-authenticated. 
     */
    REFRESHING,
    
    /**
     * The License Key entered has been authenticated and any modules 
     * that are valid have been activated within the client.
     */
    AUTHORIZED;
    
}
