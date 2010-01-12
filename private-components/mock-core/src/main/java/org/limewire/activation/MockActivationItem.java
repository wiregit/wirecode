package org.limewire.activation;

import java.util.Date;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;

public class MockActivationItem implements ActivationItem {

    @Override
    public Date getDateExpired() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDatePurchased() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLicenseName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getURL() {
        // TODO Auto-generated method stub
        return null;
    }
//
//    @Override
//    public boolean isSubscription() {
//        // TODO Auto-generated method stub
//        return false;
//    }
//
//    @Override
//    public String getFirstSupportedVersion() {
//        // TODO Auto-generated method stub
//        return null;
//    }

    @Override
    public ActivationID getModuleID() {
        return ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE;
    }

    @Override
    public Status getStatus() {
        return Status.EXPIRED;
    }

}
