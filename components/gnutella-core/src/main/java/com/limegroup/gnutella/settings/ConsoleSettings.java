package com.limegroup.gnutella.settings;

/**
 * Settings for the Console tab
 */
public class ConsoleSettings extends LimeProps {
    
    private ConsoleSettings() {}
    
    /**
     * The output pattern layout
     */
    public static final StringSetting CONSOLE_PATTERN_LAYOUT 
        = FACTORY.createStringSetting("CONSOLE_PATTERN_LAYOUT", "%-6r %-5p [%t] %c{2}.%M - %m%n");
    
    /**
     * The maximum number of characters
     */
    public static final IntSetting CONSOLE_IDEAL_SIZE 
        = FACTORY.createIntSetting("CONSOLE_IDEAL_SIZE", 20000);
    
    /**
     * Max Excess
     */
    public static final IntSetting CONSOLE_MAX_EXCESS 
        = FACTORY.createIntSetting("CONSOLE_MAX_EXCESS", 5000);
    
    /**
     * 
     */
    public static final BooleanSetting SHOW_INPUT_FIELD
        = FACTORY.createBooleanSetting("SHOW_INPUT_FIELD", true);
}
