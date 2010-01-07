package org.limewire.activation.api;

/**
 * Activation Modules the client knows about. 
 */
public enum ActivationID {

    PRO_MODULE,
    
    AVG_MODULE,
    
    UNKNOWN_MODULE;
    
    public static ActivationID getActivationID(int id) {
        switch(id) {
        case 0:
            return ActivationID.PRO_MODULE;
        case 1:
            return ActivationID.AVG_MODULE;
        }
        return ActivationID.UNKNOWN_MODULE;
    }
}
