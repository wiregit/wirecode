package org.limewire.ui.swing.activation;

import java.util.Date;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.ui.swing.util.I18n;

public class LostLicenseItem implements ActivationItem {

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
        return I18n.tr("Lost your License?");
    }

    @Override
    public String getURL() {
        return "www.limewire.com";
    }

    @Override
    public ActivationID getModuleID() {
        return ActivationID.UNKNOWN_MODULE;
    }

    @Override
    public Status getStatus() {
        return Status.UNAVAILABLE;
    }
}
