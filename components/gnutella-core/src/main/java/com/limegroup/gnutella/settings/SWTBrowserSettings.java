package com.limegroup.gnutella.settings; 

import org.limewire.setting.BooleanSetting;

/**
 * Settings for the SWT browser. This will allow us to enable or disable the
 * browser.
 */ 
public final class SWTBrowserSettings extends LimeProps {
    
    private SWTBrowserSettings() {}
    
    /** 
     * Setting for whether or not to allow multiple instances of LimeWire. 
     */ 
    public static final BooleanSetting USE_SWT_BROWSER = 
        FACTORY.createRemoteBooleanSetting("USE_SWT_BROWSER", true, "SWTBrowserSettings.useSwtBrowser"); 

}
