package com.limegroup.gnutella.settings;

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
}
