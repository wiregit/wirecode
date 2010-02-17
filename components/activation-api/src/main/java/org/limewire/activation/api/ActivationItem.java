package org.limewire.activation.api;

import java.util.Date;


/**
 * An ActivationItem represents a single Module. This item represents 
 * data retrieved from the server and whether this module can be
 * currently used.
 */
public interface ActivationItem {

    /**
     * Indicates the current status of this ActivationItem. 
     */
    static enum Status {
        
        /** ActivationItem can be used. */
        ACTIVE, 
        
        /** ActivationItem expired and must be repurchased for continued use. 
         *  If an item is expired, clients will no longer be able to see if it's also
         *  unuseable for the current os, for the current version of LimeWire, or unavailable.
         **/
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
