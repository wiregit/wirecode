package com.limegroup.gnutella.settings; 

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for the SWT browser. This will allow us to enable or disable the
 * browser.
 */ 
public final class SWTBrowserSettings extends LimeProps {
    
    private SWTBrowserSettings() {}
    
    /** 
     * Setting for whether or not to use the SWT browser.
     */ 
    public static final BooleanSetting USE_SWT_BROWSER = 
        FACTORY.createRemoteBooleanSetting("USE_SWT_BROWSER", true, "SWTBrowserSettings.useSwtBrowser"); 
    
    /** 
     * Remote String for The Lime Wire Store
     */ 
    public static final StringSetting REMOTE_LIME_WIRE_STORE_URL =
        FACTORY.createRemoteStringSetting("REMOTE_LIME_WIRE_STORE_URL", 
                                          "http://limewire.com/store", 
                                          "SWTBrowserSettings.remoteLimeWireStoreUrl");
    
    /** 
     * Local String for The Lime Wire Store
     */ 
    public static final StringSetting LOCAL_LIME_WIRE_STORE_URL =
        FACTORY.createRemoteStringSetting("LOCAL_LIME_WIRE_STORE_URL", 
                                          "http://localhost:8080/store/app/pages/Welcome",
                                          "SWTBrowserSettings.localLimeWireStoreUrl");

}
