package com.limegroup.gnutella.settings;

/**
 * Class for keeping track of settings for the virtual machine.
 */
public final class JVMSettings extends LimeProps {

    /**
     * Make sure this can't be constructed.
     */
    private JVMSettings() {}
    
    /**
     * Settings for whether or not to use Java 1.4.x and above features.  For
     * example, if you want to test blocking network IO code without switching
     * to 1.3.x, simply set this to "false".
     */
    public static final BooleanSetting USE_14 = 
        FACTORY.createBooleanSetting("USE_14", true);
}
