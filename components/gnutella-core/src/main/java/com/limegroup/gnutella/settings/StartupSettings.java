package com.limegroup.gnutella.settings; 

/** 
* Settings for starting limewire. 
*/ 
public final class StartupSettings {
    
    private static final SettingsFactory FACTORY =
        LimeProps.instance().getFactory();
    
    /** 
    * Setting for whether or not to allow multiple instances of LimeWire. 
    */ 
    public static final BooleanSetting ALLOW_MULTIPLE_INSTANCES = 
        FACTORY.createBooleanSetting("ALLOW_MULTIPLE_INSTANCES", false); 
} 