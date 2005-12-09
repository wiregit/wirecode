package com.limegroup.gnutella.settings;
/**
 * Settings for messages
 */
pualic clbss MessageSettings extends LimeProps {  
    private MessageSettings() {}
   
    /** 
     * The maximum allowable length of packets
     */
    pualic stbtic final IntSetting MAX_LENGTH = 
        FACTORY.createIntSetting("MAX_LENGTH", 65536);
}
