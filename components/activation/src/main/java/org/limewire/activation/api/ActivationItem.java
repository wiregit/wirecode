package org.limewire.activation.api;

import java.net.URL;

//TODO: This API may change, these methods are partly brainstorming for items
//      needed to be retrieved from the server and stored on the server.
public interface ActivationItem {

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
     * Returns true if this item was purchased but is now
     * expired, false otherwise.
     */
    public boolean isExpired();
    
    /**
     * Returns true if this item can be used in this
     * version of LimeWire, false otherwise.
     */
    public boolean isActiveVersion();
    
    /**
     * Returns true if this item is auto-renewing, false otherwise.
     */
    public boolean isSubscription();
    
    /**
     * Returns a URL related to this feature.
     */
    public URL getURL();

}
