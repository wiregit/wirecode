package org.limewire.activation;

import org.limewire.activation.api.ActivationID;
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
    public String getURL() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isUseable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isActive() {
        // TODO Auto-generated method stub
        return false;
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
        return ActivationID.PRO_MODULE;
    }

}
