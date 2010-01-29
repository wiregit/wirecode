package org.limewire.ui.swing.activation;

import org.limewire.activation.api.ActivationItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.util.LimeWireUtils;

public class ActivationUtilities {

    /**
     * Returns a message based on the ActivationItem's current Status.
     */
    public static String getStatusMessage(ActivationItem item) {
        switch(item.getStatus()) {
        case UNAVAILABLE:
            return I18n.tr("{0} is no longer supported by LimeWire.", item.getLicenseName());
        case UNUSEABLE_LW:
            String lwVersion = LimeWireUtils.getLimeWireVersion();
            return I18n.tr("{0} is not supported by LimeWire {1}. Please upgrade to the latest version.", item.getLicenseName(), lwVersion);
        case UNUSEABLE_OS:
            String osName = OSUtils.getOS();
            String osVersion = OSUtils.getOSVersion();
            return I18n.tr("{0} is not supported by " + osName + " " + osVersion + "." + I18n.tr(" We apologize for the inconvenience."), item.getLicenseName());
        case EXPIRED:
            return I18n.tr("{0} is expired.", item.getLicenseName());
        default:
            return "";
        }
    }

}
