package com.limegroup.gnutella.settings;

import java.io.File;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * Settings for security
 */
public class SecuritySettings extends LimeProps {

    private SecuritySettings() {}

    /**
     * A flag indicating whether this node should accept
     * only authenticated connections
     */
    public static final BooleanSetting ACCEPT_AUTHENTICATED_CONNECTIONS_ONLY = 
            FACTORY.createBooleanSetting("ACCEPT_AUTHENTICATED_CONNECTIONS_ONLY", false);

    /**
     * Name of the file that stores cookies
     */
    public static final StringSetting COOKIES_FILE = 
        FACTORY.createStringSetting("COOKIES_FILE", CommonUtils.getUserSettingsDir() + File.separator + "Cookies.dat");
        
    /**
     * Whether or not a password is required for file-view pages.
     */
    public static final BooleanSetting FILE_VIEW_USE_PASSWORD =
        FACTORY.createBooleanSetting("FILE_VIEW_USE_PASSWORD", true);
}
