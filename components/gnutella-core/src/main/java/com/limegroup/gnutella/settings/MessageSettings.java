package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;

/**
 * Settings for messages
 */
public class MessageSettings extends LimeProps {  
    private MessageSettings() {}
   
    /** 
     * The maximum allowable length of packets
     */
    public static final IntSetting MAX_LENGTH = 
        FACTORY.createIntSetting("MAX_LENGTH", 65536);
    
    /**
     * Whether to report stats with double precision.
     */
    public static final BooleanSetting REPORT_DOUBLE_PRECISION = 
        FACTORY.createRemoteBooleanSetting("REPORT_DOUBLE_PRECISION", 
                true, "MessageSettings.reportDoublePrecision");
    
    /**
     * Whether to embed a timestamp in the query guids.
     */
    public static final BooleanSetting STAMP_QUERIES =
        FACTORY.createRemoteBooleanSetting("STAMP_QUERIES", false, 
                "MessageSettings.stampQueries");
    
    /**
     * The latest handled routable version of the inspection message.
     */
    public static final IntSetting INSPECTION_VERSION = 
        FACTORY.createIntSetting("INSPECTION_VERSION", 0);
}
