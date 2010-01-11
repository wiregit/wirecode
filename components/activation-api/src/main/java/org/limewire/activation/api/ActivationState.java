package org.limewire.activation.api;

/**
 * States the ActivationManager can cycle through. 
 * 
 * The state machine behaves this way:
 * 
 *       UNINITIALIZED
 *            @------------|
 *                         V  ACTIVATING
 *    |--------------------@---------------->@  ACTIVATED
 *    | ActivationError    ^
 *    V                    |
 *    @--------------------|
 *   NOT_ACTIVATED
 *   
 *   or
 *   
 *   PROVISIONALLY_ACTIVATED 
 *   
 */
public enum ActivationState {
    
    /**
     * State when program starts. If a key already exists it has not yet
     * attempted to contact the server. Once an activation is attempted,
     * this state can never be returned to unless the program is restarted.
     */
    UNINITIALIZED,
    
    /**
     * Same state as UNINITIALIZED except the server has already been contacted,
     * with the key information and has failed. Unless a new key is entered or the
     * user takes some action, the state will remain here.
     */
    NOT_ACTIVATED,
    
    /**
     * Same state as ACTIVATED except the server could not be contacted.
     * So, the last saved state was read from disk and we continue to 
     * try to contact the server to verify the client's activation.
     */
    PROVISIONALLY_ACTIVATED,

    /**
     * ActivationManager is attempting to validate the key. If the key is valid,
     * this will transition to a ACTIVATED state or a NOT_ACTIVATED state.
     */
    ACTIVATING,

    /**
     * ActivationManager has successfully authenticated the key for this session.
     */
    ACTIVATED;   
}
