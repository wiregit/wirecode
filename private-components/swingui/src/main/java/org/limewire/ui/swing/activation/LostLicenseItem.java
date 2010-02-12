package org.limewire.ui.swing.activation;

import java.util.Date;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationSettingsController;
import org.limewire.ui.swing.util.I18n;

/**
 * An ActivationItem that is used when no items exist in the
 * ActivationTable.
 */
public final class LostLicenseItem implements ActivationItem {

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
        return I18n.tr("Don't see your features?");
    }

    @Override
    public String getURL() {
        return ActivationSettingsController.ACCOUNT_SETTINGS_URL;
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
