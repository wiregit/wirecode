package org.limewire.activation.api;

import java.util.Date;


/**
 * An ActivationItem represents a single Module. This item represents 
 * data retrieved from the server and whether this module can be
 * currently used.
 */
public interface ActivationItem {

    static enum Status {
        
        /** ActivationItem can be used. */
        ACTIVE, 
        
        /** ActivationItem expired and must be repurchased for continued use. */
        EXPIRED, 
        
        /** ActivationItem is Active but can't be used by this Operating System. */
        UNUSEABLE_OS, 
        
        /** ActivationItem is Active but can't be used by this version of LW. */
        UNUSEABLE_LW, 
        
        /** ActivationItem is no longer supported by LW. */
        UNAVAILABLE
    }
    
    /**
     * Returns the ActivationID associated with this item.
     * @return
     */
    public ActivationID getModuleID();
    
    /**
     * Returns a user facing name for this item.
     */
    public String getLicenseName();
    
    /**
     * Returns the date that this item was purchased.
     */
    public Date getDatePurchased();
    
    /**
     * Returns the date that this item expires.
     */
    public Date getDateExpired();
    
    /**
     * Returns the status that this module is in for this ActivationKey.
     */
    public Status getStatus();

    /**
     * Returns a URL related to this feature.
     */
    public String getURL();
}
