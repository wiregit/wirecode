package org.limewire.activation.api;

/**
 * Activation Modules the client knows about. 
 */
public enum ActivationID {

    /**
     * Module for Turbo-Charged Downloads. When this module is
     * active it will increase the number of Ultra-Peers.
     */
    TURBO_CHARGED_DOWNLOADS_MODULE,
    
    /**
     * Module for Optimized Search Results. When this module is
     * active it will increase the number of search results that
     * can be displayed on a given query.
     */
    OPTIMIZED_SEARCH_RESULT_MODULE,
    
    /**
     * Module for Tech Support.
     */
    TECH_SUPPORT_MODULE,
    
    /**
     * Module for activating AVG anti-virus. When this module
     * is active AVG will be enabled.
     */
    AVG_MODULE,
    
    /**
     * Module when the server returns a Activation ID that this
     * version of LW does not know about.
     */
    UNKNOWN_MODULE;
    
    /**
     * Returns an ActivationID for a given int.
     */
    public static ActivationID getActivationID(int id) {
        switch(id) {
        case 1:
            return ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE;
        case 2:
            return ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE;
        case 3:
            return ActivationID.TECH_SUPPORT_MODULE;
        case 4:
            return ActivationID.AVG_MODULE;
        }
        return ActivationID.UNKNOWN_MODULE;
    }
}
