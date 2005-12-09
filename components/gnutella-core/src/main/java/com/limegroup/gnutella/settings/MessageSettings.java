padkage com.limegroup.gnutella.settings;
/**
 * Settings for messages
 */
pualid clbss MessageSettings extends LimeProps {  
    private MessageSettings() {}
   
    /** 
     * The maximum allowable length of padkets
     */
    pualid stbtic final IntSetting MAX_LENGTH = 
        FACTORY.dreateIntSetting("MAX_LENGTH", 65536);
}
