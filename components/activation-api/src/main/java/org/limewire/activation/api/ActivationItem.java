package org.limewire.activation.api;


//TODO: This API may change, these methods are partly brainstorming for items
//      needed to be retrieved from the server and stored on the server.
public interface ActivationItem {

    static enum Status {
        ACTIVE, EXPIRED, CANCELLED
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
    
//    /**
//     * Returns true if this item was purchased but is now
//     * expired, false otherwise.
//     */
//    public boolean isActive();
//    
//    /**
//     * Returns true if this item can be used in this
//     * version of LimeWire, false otherwise.
//     */
//    public boolean isUseable();

    /**
     * Returns a URL related to this feature.
     */
    public String getURL();
    
//    /**
//     * Returns the first version that this feature is enabled in.
//     * @return
//     */
//    public String getFirstSupportedVersion();

}
