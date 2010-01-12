package org.limewire.activation.api;


//TODO: This API may change, these methods are partly brainstorming for items
//      needed to be retrieved from the server and stored on the server.
public interface ActivationItem {

    static enum Status {
        ACTIVE, EXPIRED, UNUSEABLE_OS, UNUSEABLE_LW, UNAVAILABLE
    }
    
    public ActivationID getModuleID();
    
    /**
     * Returns a user facing name for this item.
     */
    public String getLicenseName();
    
    /**
     * Returns the date that this item was purchased.
     */
    public long getDatePurchased();
    
    /**
     * Returns the date that this item expires.
     */
    public long getDateExpired();
    
    /**
     * Returns the status that this module is in for this ActivationKey.
     * @return
     */
    public Status getStatus();

    /**
     * Returns a URL related to this feature.
     */
    public String getURL();
}
