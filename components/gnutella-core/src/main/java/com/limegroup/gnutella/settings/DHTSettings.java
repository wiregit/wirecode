package com.limegroup.gnutella.settings;

public class DHTSettings extends LimeProps{

    private DHTSettings() {}

    /**
     * Setting for whether or not we are DHT capable.
     */
    public static final BooleanSetting DHT_CAPABLE =
        FACTORY.createExpirableBooleanSetting("EVER_DHT_CAPABLE", false);
    
    /**
     * Setting for whether or not we've ever been Ultrapeer capable.
     */
    public static final BooleanSetting FORCE_DHT_CONNECT =
        FACTORY.createSettableBooleanSetting("FORCE_DHT_CONNECT", false, "DHTSettings.ForceDHTConnect");

    /**
     * Setting for wether or not the DHT should be active at all.
     */
    public static final BooleanSetting DISABLE_DHT =
        FACTORY.createSettableBooleanSetting("DISABLE_DHT", false, "DHTSettings.DisableDHT"); //TODO switch to true

    /**
     * Setting for the minimum average uptime required to join the DHT.
     */
    public static final IntSetting MIN_DHT_AVG_UPTIME =
//        FACTORY.createSettableIntSetting("MIN_DHT_AVG_UPTIME",3600,"DHTSettings.MinDHTAvgUptime",3600,48*3600);
        FACTORY.createSettableIntSetting("MIN_DHT_AVG_UPTIME",1,"DHTSettings.MinDHTAvgUptime",1,48*3600); //TODO rechange
    /**
     * The minimum current uptime in seconds that a node must have to join the DHT.
     */
    public static final IntSetting MIN_DHT_INITIAL_UPTIME =
        FACTORY.createSettableIntSetting("MIN_DHT_INITIAL_UPTIME",120*60,"DHTSettings.MinDHTInitialUptime",120*60,48*3600);
    
    /**
     * Setting for whether or not an ultrapeer can join the DHT.
     */
    public static final BooleanSetting EXCLUDE_ULTRAPEERS =
        FACTORY.createBooleanSetting("EXCLUDE_ULTRAPEERS", true);
    
    /**
     * Setting for the probability to switch from DHT node to Ultrapeer node
     */
    public static final FloatSetting DHT_TO_ULTRAPEER_PROBABILITY =
        FACTORY.createSettableFloatSetting("DHT_TO_ULTRAPEER_PROBABILITY", 0F, "DHTSettings.DHTToUltrapeerProbability",0F,1F);
}
