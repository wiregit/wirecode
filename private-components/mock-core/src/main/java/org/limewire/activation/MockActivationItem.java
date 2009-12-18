package org.limewire.activation;

import java.net.URL;

import org.limewire.activation.api.ActivationItem;

public class MockActivationItem implements ActivationItem {

    @Override
    public long getDateExpired() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getDatePurchased() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getLicenseName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getURL() {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return false;
    }

}
