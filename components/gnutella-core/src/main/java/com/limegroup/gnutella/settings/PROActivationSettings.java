package com.limegroup.gnutella.settings;

import org.limewire.setting.StringSetting;

public class PROActivationSettings extends LimeProps {
    
    /**
     * A client identifier for a pro activation request if user has started the activation process.
     */
    public static StringSetting PRO_ACTIVATION_ID = 
        FACTORY.createStringSetting("PRO_ACTIVATION_ID", "");
    
    /**
     * The date of the local users activation key ID if he has created one.
     */
    public static StringSetting PRO_ACTIVATION_ID_DATE = 
        FACTORY.createStringSetting("PRO_ACTIVATION_ID_DATE", "");
    
    /**
     * The actual string representation of an activation key.
     */
    public static StringSetting PRO_ACTIVATION_KEY = 
        FACTORY.createStringSetting("PRO_ACTIVATION_KEY", "");

    // TODO:  These URLS should probably don't need to be remote.  Should be based off a subdomain.
    //        Need a third one for expiry.
    /**
     * The URL to send a user to when wishing to activate PRO.
     */
    public static final StringSetting PRO_ACTIVATION_START_URL = 
        FACTORY.createRemoteStringSetting
        ("PRO_ACTIVATION_START_URL","http://stage.limewire.com/cgi-bin/proUpgrade.cgi","proActivationStartURL");
    
    /**
     * The lookup URL for testing if an activation key is good.
     */
    public static final StringSetting PRO_ACTIVATION_LOOKUP_URL = 
        FACTORY.createRemoteStringSetting
        ("PRO_ACTIVATION_LOOKUP_URL","http://stage.limewire.com/cgi-bin/proLookup.cgi","proActivationLookupURL");
}
