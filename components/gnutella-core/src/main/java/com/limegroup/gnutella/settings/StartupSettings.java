package com.limegroup.gnutella.settings; 

/** 
 * Settings for starting limewire. 
 */ 
public final class StartupSettings extends LimeProps {
    
    private StartupSettings() {}
    
    /** 
     * Setting for whether or not to allow multiple instances of LimeWire. 
     */ 
    public static final BooleanSetting ALLOW_MULTIPLE_INSTANCES = 
        FACTORY.createBooleanSetting("ALLOW_MULTIPLE_INSTANCES", false); 
        
    /**
     * A boolean flag for whether or not to start LimeWire on system startup.
     */
    public static final BooleanSetting RUN_ON_STARTUP = 
        FACTORY.createBooleanSetting("RUN_ON_STARTUP", true);
}
