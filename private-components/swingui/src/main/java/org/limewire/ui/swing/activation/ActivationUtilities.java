package org.limewire.ui.swing.activation;

import org.limewire.activation.api.ActivationItem;
import org.limewire.ui.swing.util.I18n;

public class ActivationUtilities {

    /**
     * Returns a message based on the ActivationItem's current Status.
     */
    public static String getStatusMessage(ActivationItem item) {
        switch(item.getStatus()) {
        case UNAVAILABLE:
            return I18n.tr("{0} is no longer supported by LimeWire.", item.getLicenseName());
        case UNUSEABLE_LW:
            return I18n.tr("{0} is not supported in this LimeWire version. Please upgrade LimeWire to access this featured.", item.getLicenseName());
        case UNUSEABLE_OS:
            return I18n.tr("{0} is not supported for this Operating System.", item.getLicenseName());
        default:
            return "";
        }
    }
}
