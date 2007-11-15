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
     * Remote String for The LimeWire Store
     */ 
    public static final StringSetting REMOTE_LIME_WIRE_STORE_URL =
        FACTORY.createRemoteStringSetting("REMOTE_LIME_WIRE_STORE_URL", 
                                          "http://limewire.com/store", 
                                          "SWTBrowserSettings.remoteLimeWireStoreUrl");
    
     /**
      * The name of the browser in the tab.
      */
    public static final StringSetting SWT_BROWSER_TITLE =
        FACTORY.createRemoteStringSetting("SWT_BROWSER_TITLE", 
                                          "Browse the Web", 
                                          "SWTBrowserSettings.swtBrowserTitle");
}
