package org.limewire.activation.impl;

import java.net.URL;

import org.limewire.activation.api.ActivationItem;

public class ActivationItemImpl implements ActivationItem {

    private String licenseName;
    private long datePurchased;
    private long dateExpired;
    private boolean isExpired;
    private URL url;
    private boolean isSubscription;
    
    
    @Override
    public long getDateExpired() {
        return dateExpired;
    }
    @Override
    public long getDatePurchased() {
        return datePurchased;
    }
    @Override
    public String getLicenseName() {
        return licenseName;
    }
    @Override
    public URL getURL() {
        return url;
    }
    @Override
    public boolean isActiveVersion() {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean isExpired() {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean isSubscription() {
        return isSubscription;
    }
}
