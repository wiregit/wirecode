package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for The Lime Wire Store&#8482;. This is used by
 * {@link LWSManagerImpl} for the host name to which we connect for
 * authentication.
 */
public final class LWSSettings extends LimeProps {
    
    
    private LWSSettings() {}

    /**
     * The hostname to which we connect for authentication.
     */
    public static final StringSetting AUTHENTICATION_HOSTNAME = FACTORY.createRemoteStringSetting(
            "AUTHENTICATION_HOSTNAME", "", "LWSSettings.authenticationHostname");

    /**
     * The port on which we connect for authentication. This can be
     * <code><= 0</code> for no port.
     */
    public static final IntSetting AUTHENTICATION_PORT = FACTORY.createRemoteIntSetting(
            "AUTHENTICATION_PORT", 8080, "LWSSettings.authenticationPort", -Integer.MIN_VALUE,
            10000);

    /**
     * Allow us to disable the lws server.
     */
    public static final BooleanSetting IS_ENABLED = FACTORY.createRemoteBooleanSetting(
            "IS_ENABLED", true, "LWSSettings.isEnabled");

}
