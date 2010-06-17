package org.limewire.core.settings;

import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectionPoint;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringArraySetting;

/** Settings related to content management. */
public class ContentSettings extends LimeProps {    
    private ContentSettings() {}
    
    /** The list of default content authorities. */
    public static final StringArraySetting AUTHORITIES =
        FACTORY.createRemoteStringArraySetting("CONTENT_AUTHORITIES", new String[0], "content.authorities");
    
    /** Whether or not we want to use content management. */
    @InspectionPoint(value = "content management active", category = DataCategory.USAGE)
    public static final BooleanSetting CONTENT_MANAGEMENT_ACTIVE =
        FACTORY.createRemoteBooleanSetting("CONTENT_MANAGEMENT_ACTIVE", true, "content.managementActive");
    
    /**
     * Whether or not the user is enabling management.
     * Both this & the above must be on for management to be active.
     */
    @InspectionPoint(value = "user wants filter management", category = DataCategory.USAGE)
    public static final BooleanSetting USER_WANTS_MANAGEMENTS =
        FACTORY.createBooleanSetting("USER_WANTS_MANAGEMENTS", true);
    
    /**
     * Returns true if content management is active.
     * 
     * @return
     */
    public static boolean isManagementActive() {
        return CONTENT_MANAGEMENT_ACTIVE.getValue() && USER_WANTS_MANAGEMENTS.getValue();
    }
}

