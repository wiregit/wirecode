package com.limegroup.gnutella.settings;

/**
 * Settings to deal with bugs
 */ 
public final class BugSettings extends LimeProps {

    private BugSettings() {}

    /**
     * Setting for whether or not to automatically report bugs
     * to the bug servlet.
     */
    public static final BooleanSetting USE_BUG_SERVLET =
		FACTORY.createBooleanSetting("USE_BUG_SERVLET", false);
		
    /**
     * Setting for whether or not to completely ignore all bugs.
     */
    public static final BooleanSetting IGNORE_ALL_BUGS =
        FACTORY.createBooleanSetting("IGNORE_ALL_BUGS", false);
}
