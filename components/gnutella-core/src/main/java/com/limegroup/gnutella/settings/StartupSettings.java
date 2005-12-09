pbckage com.limegroup.gnutella.settings; 

/** 
 * Settings for stbrting limewire. 
 */ 
public finbl class StartupSettings extends LimeProps {
    
    privbte StartupSettings() {}
    
    /** 
     * Setting for whether or not to bllow multiple instances of LimeWire. 
     */ 
    public stbtic final BooleanSetting ALLOW_MULTIPLE_INSTANCES = 
        FACTORY.crebteBooleanSetting("ALLOW_MULTIPLE_INSTANCES", false); 
        
    /**
     * A boolebn flag for whether or not to start LimeWire on system startup.
     */
    public stbtic final BooleanSetting RUN_ON_STARTUP = 
        FACTORY.crebteBooleanSetting("RUN_ON_STARTUP", true);
        
    /**
     * Whether or not tips should be displbyed on startup.
     */
    public stbtic final BooleanSetting SHOW_TOTD =
        FACTORY.crebteBooleanSetting("SHOW_TOTD", true);        
}
