package org.limewire.activation;

import java.util.Date;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;

public class MockActivationItem implements ActivationItem {

    @Override
    public Date getDateExpired() {
        return null;
    }

    @Override
    public Date getDatePurchased() {
        return null;
    }

    @Override
    public String getLicenseName() {
        return null;
    }

    @Override
    public String getURL() {
        return null;
    }

    @Override
    public ActivationID getModuleID() {
        return ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE;
    }

    @Override
    public Status getStatus() {
        return Status.EXPIRED;
    }

}
