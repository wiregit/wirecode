package com.limegroup.gnutella.settings;

/** Settings related to content management. */
public class ContentSettings extends LimeProps {    
    private ContentSettings() {}
    
    public static String LEARN_MORE_URL = "http://filtered.limewire.com/learnmore/contentFiltering";
    
    /** The list of default content authorities. */
    public static final StringArraySetting AUTHORITIES =
        FACTORY.createSettableStringArraySetting("CONTENT_AUTHORITIES", new String[0], "content.authorities");
    
    /** Whether or not we want to use content management. */
    public static final BooleanSetting CONTENT_MANAGEMENT_ACTIVE =
        FACTORY.createSettableBooleanSetting("CONTENT_MANAGEMENT_ACTIVE", false, "content.managementActive");
    
    /**
     * Whether or not the user is enabling management.
     * Both this & the above must be on for management to be active.
     */
    public static final BooleanSetting USER_WANTS_MANAGEMENTS =
        FACTORY.createBooleanSetting("CONTENT_USER_MANAGEMENT_ACTIVE", false);
    
    /**
     * Whether or not we're only accepting ContentResponses that
     * are properly signed.
     */
    public static final BooleanSetting ONLY_SECURE_CONTENT_RESPONSES
        = FACTORY.createBooleanSetting("ONLY_SECURE_CONTENT_RESPONSES", true);
    
    /**
     * Returns true if content management is active.
     * 
     * @return
     */
    public static boolean isManagementActive() {
        return CONTENT_MANAGEMENT_ACTIVE.getValue() && USER_WANTS_MANAGEMENTS.getValue();
    }
}

