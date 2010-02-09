package org.limewire.ui.swing.activation;

import java.awt.Font;
import java.net.URL;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.limewire.activation.api.ActivationItem;
import org.limewire.core.api.Application;
import org.limewire.core.settings.ActivationSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.OSUtils;

public class ActivationUtilities {

    /**
     * Returns a message based on the ActivationItem's current Status.
     */
    public static JComponent getStatusMessage(ActivationItem item, Application application) {
        switch(item.getStatus()) {
        case UNAVAILABLE:
            return new MultiLineLabel(I18n.tr("{0} is no longer supported by LimeWire.", item.getLicenseName()));
        case UNUSEABLE_LW:
            String lwVersion = application.getVersion();
            LabelWithLinkSupport textLabel = new LabelWithLinkSupport();
            Font font = new MultiLineLabel().getFont();
            textLabel.setText("<html>" + "<font size=\"4\" face=\"" + font.getFontName() + "\">" 
                                + I18n.tr("{0} is not supported by LimeWire {1}. ", item.getLicenseName(), lwVersion) 
                                + I18n.tr("Please {0}upgrade{1} to the latest version.", "<a href='" + ActivationSettings.LIMEWIRE_DOWNLOAD_UPDATE_HOST.get() + "'>", "</a>")
                                + "</html>");
            return textLabel;
        case UNUSEABLE_OS:
            String osName = OSUtils.getOS();
            String osVersion = OSUtils.getOSVersion();
            MultiLineLabel label = new MultiLineLabel(I18n.tr("{0} is not supported by {1} {2}. We apologize for the inconvenience.", item.getLicenseName(), osName, osVersion));
            return label;
        case EXPIRED:
            return new MultiLineLabel(I18n.tr("{0} is expired.", item.getLicenseName()));
        default:
            return new MultiLineLabel("");
        }
    }
    
    public static String getInfoIconURL() {
        return getURL("/org/limewire/ui/swing/mainframe/resources/icons/info.png");
    }

    private static String getURL(String path) {
        URL resource = ActivationUtilities.class.getResource(path);
        return resource != null ? resource.toExternalForm() : "";
    }

}
