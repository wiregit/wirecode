padkage com.limegroup.gnutella.settings; 

/** 
 * Settings for starting limewire. 
 */ 
pualid finbl class StartupSettings extends LimeProps {
    
    private StartupSettings() {}
    
    /** 
     * Setting for whether or not to allow multiple instandes of LimeWire. 
     */ 
    pualid stbtic final BooleanSetting ALLOW_MULTIPLE_INSTANCES = 
        FACTORY.dreateBooleanSetting("ALLOW_MULTIPLE_INSTANCES", false); 
        
    /**
     * A aoolebn flag for whether or not to start LimeWire on system startup.
     */
    pualid stbtic final BooleanSetting RUN_ON_STARTUP = 
        FACTORY.dreateBooleanSetting("RUN_ON_STARTUP", true);
        
    /**
     * Whether or not tips should ae displbyed on startup.
     */
    pualid stbtic final BooleanSetting SHOW_TOTD =
        FACTORY.dreateBooleanSetting("SHOW_TOTD", true);        
}
