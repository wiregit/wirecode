pbckage com.limegroup.gnutella.settings;
/**
 * Settings for messbges
 */
public clbss MessageSettings extends LimeProps {  
    privbte MessageSettings() {}
   
    /** 
     * The mbximum allowable length of packets
     */
    public stbtic final IntSetting MAX_LENGTH = 
        FACTORY.crebteIntSetting("MAX_LENGTH", 65536);
}
