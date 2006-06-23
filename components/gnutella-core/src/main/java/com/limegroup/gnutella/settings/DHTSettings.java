package com.limegroup.gnutella.settings;

public class DHTSettings extends LimeProps{

    private DHTSettings() {}

    /**
     * Setting for whether or not we are DHT capable.
     */
    public static final BooleanSetting DHT_CAPABLE =
        FACTORY.createExpirableBooleanSetting("EVER_DHT_CAPABLE", false);
    
    /**
     * Setting to force DHT capability -- TODO for testing only - remove.
     */
    public static final BooleanSetting FORCE_DHT_CONNECT =
        FACTORY.createSettableBooleanSetting("FORCE_DHT_CONNECT", false, "DHTSettings.ForceDHTConnect"); //TODO switch to false

    /**
     * Setting for wether or not the DHT should be active at all.
     */
    public static final BooleanSetting DISABLE_DHT_USER =
        FACTORY.createBooleanSetting("DISABLE_DHT_USER", false); //TODO switch to true

    /**
     * Setting for wether or not the DHT should be active at all.
     */
    public static final BooleanSetting DISABLE_DHT_NETWORK =
        FACTORY.createSettableBooleanSetting("DISABLE_DHT_NETWORK", false, "DHTSettings.DisableDHT"); //TODO switch to true
    
    /**
     * Setting for the minimum average uptime (in seconds) required to join the DHT.
     */
    public static final IntSetting MIN_DHT_AVG_UPTIME =
        FACTORY.createSettableIntSetting("MIN_DHT_AVG_UPTIME",120*60,"DHTSettings.MinDHTAvgUptime",120*60,48*3600);
    /**
     * The minimum current uptime (in seconds) that a node must have to join the DHT.
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
    
    public static final BooleanSetting PERSIST_DHT = 
        FACTORY.createBooleanSetting("PERSIST_DHT", true);
}
