package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for geo coding the client
 */
public class GeocodeSettings extends LimeProps {
    
    private GeocodeSettings() {}
                                                        
	/**
	 * Whether geo coding is enabled.
	 */
	public static final BooleanSetting IS_ENABLED =
		FACTORY.createRemoteBooleanSetting("IS_ENABLED", true, "GeocodeSettings.isEnabled");
    
    /**
     * The URL used to geocode the client.
     */
    public static final StringSetting GEOCODE_URL =
        FACTORY.createRemoteStringSetting("GEOCODE_URL", "http://jeffpalm.com/geotest/", "GeocodeSettings.geocodeUrl");    
    
    /**
     * The last IP address from which we opened the client. Don't know if it's
     * <code>null</code> or empty.
     */
    public static final StringSetting LAST_IP =
        FACTORY.createRemoteStringSetting("LAST_IP", "", "GeocodeSettings.lastIp");
    
    /**
     * The time out to use when requesting the geolocation information.
     */
    public static final IntSetting TIMEOUT =
        FACTORY.createRemoteIntSetting("TIMEOUT", 10000, "GeocodeSettings.timeout", 0, 20000);
}
