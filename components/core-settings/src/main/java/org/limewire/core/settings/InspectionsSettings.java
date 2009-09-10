package org.limewire.core.settings;

import org.limewire.setting.IntSetting;
import org.limewire.setting.ProbabilisticBooleanSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings related to push inspections
 */
public class InspectionsSettings extends LimeProps {

    /**
     * Setting for whether or not push inspections are enabled
     */
    public static final ProbabilisticBooleanSetting PUSH_INSPECTIONS_ON =
        FACTORY.createRemoteProbabilisticBooleanSetting("PUSH_INSPECTIONS_ON", 
                0f, "Inspection.on", 0f, 1f);
    
    
    /**
     * Inspection server URL(s)
     */
    public static final StringSetting INSPECTION_SPEC_REQUEST_URL = 
        FACTORY.createRemoteStringSetting("INSPECTION_SPEC_REQUEST_URL", 
                "http://localhost:9876", "Inspection.requestUrl");
    
    public static final StringSetting INSPECTION_SPEC_SUBMIT_URL = 
        FACTORY.createRemoteStringSetting("INSPECTION_SPEC_SUBMIT_URL", 
                "http://localhost:9876", "Inspection.submitUrl");

    /**
     * The minimum interval between scheduled inspections in seconds
     */
    public static final IntSetting INSPECTION_SPEC_MINIMUM_INTERVAL = 
        FACTORY.createRemoteIntSetting("INSPECTION_SPEC_MINIMUM_INTERVAL", 
                60, "Inspection.minInterval", 0, Integer.MAX_VALUE);
}
