package com.limegroup.gnutella.settings;

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
    public static final StringSetting AUTHENTICATION_HOSTNAME =
        FACTORY.createRemoteStringSetting("AUTHENTICATION_HOSTNAME", "", "authenticationHostname");
    
    /**
     * The on which we connect for authentication.  This can be <code><= 0</code> for no port.
     */    
    public static final IntSetting AUTHENTICATION_PORT =
        FACTORY.createRemoteIntSetting("AUTHENTICATION_PORT", 8080, "authenticationPort", -Integer.MIN_VALUE, 10000);  
}
