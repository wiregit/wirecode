package com.limegroup.gnutella.settings;

import org.limewire.i18n.I18nMarker;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringArraySetting;

/**
 * Settings for the startup Dialog: Upgrade to Pro.
 * Contains the message string and remote changeable values for the dialog.
 */
public class StartupAdSettings extends LimeProps {

    private StartupAdSettings(){}
    
    /** Message to display about at startup. */
    public static final StringArraySetting PRO_STARTUP_ADS = 
        FACTORY.createRemoteStringArraySetting("PRO_STARTUP_ADS", 
                new String[]{
                I18nMarker.marktr("Upgrade to PRO"),
                I18nMarker.marktr("For Turbo-Charged downloads, get LimeWire PRO. We guarantee that you will love the improved performance of PRO. Thank you for helping keep the Internet open by running LimeWire."),
                I18nMarker.marktr("Upgrade to LimeWire PRO?"),
                "http://www.limewire.com/index.jsp/pro",
                "http://www.limewire.com/promote/whygopro",
                "http://clientpix.limewire.com/pix/defaultProAd.jpg",
                "1.0f"
        },"StartupAdSettings.PRO_STARTUP_ADS");
    
    /** Whether to display the Purchase Pro dialog at startup*/
    public static final BooleanSetting PRO_STARTUP_IS_VISIBLE = 
        FACTORY.createRemoteBooleanSetting("PRO_STARTUP_IS_VISIBLE", true, "StartupAdSettings.proStartupIsVisible");
    
    /** Whether to randomize the Purchase Pro dialog buttons*/
    public static final BooleanSetting PRO_STARTUP_RANDOM_BUTTONS = 
        FACTORY.createRemoteBooleanSetting("PRO_STARTUP_RANDOM_BUTTONS", false, "StartupAdSettings.proStartupRandomButtons");
    
    /** Whether to remotely load the background image on the dialog, false display default image always*/
    public static final BooleanSetting PRO_STARTUP_REMOTE_IMAGE = 
        FACTORY.createRemoteBooleanSetting("PRO_STARTUP_REMOTE_IMAGE",  false, "StartupAdSettings.proStartupRemoteImage");
}
