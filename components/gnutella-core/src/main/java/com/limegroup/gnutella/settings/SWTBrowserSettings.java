package com.limegroup.gnutella.settings; 

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringSetting;

import com.limegroup.gnutella.util.LimeWireUtils;

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
        FACTORY.createRemoteBooleanSetting("USE_SWT_BROWSER",
                                           true,
                                           "SWTBrowserSettings.useSwtBrowser"); 
    
    /** 
     * Remote String for The LimeWire Store
     */ 
    public static final StringSetting BROWSER_HOME_URL =
        FACTORY.createRemoteStringSetting("SWT_BROWSER_HOME_URL", 
                                          "http://www.limewire.com/client/index.php?p=b2", 
                                          "SWTBrowserSettings.remoteLimeWireStoreUrl");
    
    public static StringSetting getTitleSetting() {
        return LimeWireUtils.isPro() ? SWT_BROWSER_TITLE_WITH_AMPS_PRO :
            SWT_BROWSER_TITLE_WITH_AMPS;
    }
    
    public static StringSetting getTooltipSetting() {
        return LimeWireUtils.isPro() ? SWT_BROWSER_TOOLTIP_PRO :
            SWT_BROWSER_TOOLTIP;
    }
    
    /**
     * The name of the browser in the tab, with ampserands for better functionality.
     */
   private static final StringSetting SWT_BROWSER_TITLE_WITH_AMPS =
       FACTORY.createRemoteStringSetting("SWT_BROWSER_TITLE_WITH_AMPS", 
                                         "New @ &Lime", 
                                         "SWTBrowserSettings.swtBrowserTitleWithAmps");
   
   /**
    * The name of the browser in the tab for PRO people, with ampserands for better functionality.
    */
   private static final StringSetting SWT_BROWSER_TITLE_WITH_AMPS_PRO =
      FACTORY.createRemoteStringSetting("SWT_BROWSER_TITLE_WITH_AMPS_PRO", 
                                        "New @ &Lime", 
                                        "SWTBrowserSettings.swtBrowserTitleWithAmpsPro");
  
   private static final StringSetting SWT_BROWSER_TOOLTIP_PRO =
      FACTORY.createRemoteStringSetting("SWT_BROWSER_TOOLTIP_PRO", 
                                        "Learn More...", 
                                        "SWTBrowserSettings.swtBrowserTooltipPro");
  
    
   private static final StringSetting SWT_BROWSER_TOOLTIP =
        FACTORY.createRemoteStringSetting("SWT_BROWSER_TOOLTIP", 
                                          "Learn More...", 
                                          "SWTBrowserSettings.swtBrowserTooltip");
    
    /** Whether or not the address bar is visible. */
    public static final BooleanSetting BROWSER_SHOW_ADDRESS =
        FACTORY.createRemoteBooleanSetting("SWT_BROWSER_SHOW_ADDRESS",
                                           true,
                                           "SWTBrowserSettings.swtBrowserShowAddress");
}
