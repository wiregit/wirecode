package com.limegroup.gnutella.settings;

/** Settings related to content management. */
public class ContentSettings extends LimeProps {    
    private ContentSettings() {}
    
    /** The list of default content authorities. */
    public static final StringArraySetting AUTHORITIES =
        FACTORY.createSettableStringArraySetting("CONTENT_AUTHORITIES", new String[0], "content.authorities");
    
    /** Whether or not Content Management is on. */
    public static final BooleanSetting CONTENT_MANAGEMENT_ACTIVE =
        FACTORY.createSettableBooleanSetting("CONTENT_MANAGEMENT_ACTIVE", false, "content.managementActive");
}

