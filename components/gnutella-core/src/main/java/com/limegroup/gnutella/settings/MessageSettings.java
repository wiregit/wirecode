package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

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
     * Whether to embed a timestamp in the query guids.
     */
    public static final BooleanSetting STAMP_QUERIES =
        FACTORY.createRemoteBooleanSetting("STAMP_QUERIES", false, 
                "MessageSettings.stampQueries");
    
    /**
     * The latest handled routable version of the inspection message.
     */
    public static final LongSetting INSPECTION_VERSION = 
        FACTORY.createLongSetting("INSPECTION_VERSION", 0);
    
    /**
     * A custom criteria for evaluating FileDescs.
     */
    public static final StringArraySetting CUSTOM_FD_CRITERIA =
        FACTORY.createRemoteStringArraySetting("CUSTOM_FD_CRITERIA", 
                new String[]{"false"}, "MessageSettings.customFDCriteria");
    
    /**
     * A guid to track.
     */
    public static final StringSetting TRACKING_GUID = 
        FACTORY.createRemoteStringSetting("TRACKNG_GUID", "", "MessageSettings.trackingGUID");
    
    /**
     * Whether ttroot urns should go in ggep instead of huge
     */
    public static final BooleanSetting TTROOT_IN_GGEP = 
        FACTORY.createRemoteBooleanSetting("TTROOT_IN_GGEP", true, "MessageSettings.TTROOTInGGEP");
}
