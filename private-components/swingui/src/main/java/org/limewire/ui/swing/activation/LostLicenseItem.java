package org.limewire.ui.swing.activation;

import org.limewire.activation.api.ActivationItem;
import org.limewire.ui.swing.util.I18n;

public class LostLicenseItem implements ActivationItem {

    @Override
    public long getDateExpired() {
        return -1;
    }

    @Override
    public long getDatePurchased() {
        return -1;
    }

    @Override
    public String getFirstSupportedVersion() {
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
    public boolean isActiveVersion() {
        return false;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public boolean isSubscription() {
        return false;
    }

}
