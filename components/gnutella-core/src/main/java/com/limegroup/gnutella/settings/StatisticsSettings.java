package com.limegroup.gnutella.settings;

/**
 * Settings for Statistics related stuff.
 */ 

public class StatisticsSettings extends LimeProps {
    
    public static BooleanSetting RECORD_VM_STATS = 
        FACTORY.createBooleanSetting("RECORD_VM_STATS", false);

    /**
     * Whether or not advanced stats should be displayed by default.
     */
    public static final BooleanSetting DISPLAY_ADVANCED_STATS =
        FACTORY.createBooleanSetting("DISPLAY_ADVANCED_STATS", false);
}
