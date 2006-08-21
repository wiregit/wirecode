package com.limegroup.gnutella.settings;

public class DHTSettings extends LimeProps{

    private DHTSettings() {}
    
    /**
     * A settable list of bootstrap hosts in the host:port format
     */
    public static final StringArraySetting DHT_BOOTSTRAP_HOSTS =
        FACTORY.createSettableStringArraySetting("DHT_BOOTSTRAP_HOSTS", new String[0], "dht_bootstrap_hosts");

    /**
     * Setting for whether or not we are active DHT capable.
     */
    public static final BooleanSetting ACTIVE_DHT_CAPABLE =
        FACTORY.createExpirableBooleanSetting("EVER_DHT_CAPABLE", false);
    
    /**
     * Setting to force DHT capability -- TODO for testing only - remove.
     */
    public static final BooleanSetting FORCE_DHT_CONNECT =
    	FACTORY.createSettableBooleanSetting("FORCE_DHT_CONNECT", false, "DHTSettings.ForceDHTConnect");

    /**
     * Setting for wether or not the DHT should be active at all.
     */
    public static final BooleanSetting DISABLE_DHT_USER =
        FACTORY.createBooleanSetting("DISABLE_DHT_USER", false);

    /**
     * Setting for wether or not the DHT should be active at all.
     */
    public static final BooleanSetting DISABLE_DHT_NETWORK =
        FACTORY.createSettableBooleanSetting("DISABLE_DHT_NETWORK", true, "DHTSettings.DisableDHT");
    
    /**
     * Setting for the minimum average uptime (in ms) required to join the DHT.
     * 
     * TODO:rechange -- tests only!
     * 
     */
    public static final LongSetting MIN_DHT_AVG_UPTIME =
        FACTORY.createSettableLongSetting("MIN_DHT_AVG_UPTIME",120*60*1000L,
                "DHTSettings.MinDHTAvgUptime",0*60*1000L,48*3600*1000L);
    
    /**
     * The minimum current uptime (in ms) that a node must have to join the DHT.
     * 
     * rechange -- tests only!
     */
    public static final LongSetting MIN_DHT_INITIAL_UPTIME =
        FACTORY.createSettableLongSetting("MIN_DHT_INITIAL_UPTIME",120*60*1000L,
                "DHTSettings.MinDHTInitialUptime",0*60*1000L,48*3600*1000L);
    
    /**
     * Setting for whether or not an ultrapeer can join the DHT.
     */
    public static final BooleanSetting EXCLUDE_ULTRAPEERS =
        FACTORY.createBooleanSetting("EXCLUDE_ULTRAPEERS", true);
    
    /**
     * Setting for the probability to switch from DHT node to Ultrapeer node
     * 
     * TODO: rechange -- tests only!
     */
    public static final FloatSetting DHT_TO_ULTRAPEER_PROBABILITY =
        FACTORY.createSettableFloatSetting("DHT_TO_ULTRAPEER_PROBABILITY", 0.5F, "DHTSettings.DHTToUltrapeerProbability",0F,1F);
    
    /**
     * Setting for whether or not the DHT should be persisted on disk
     */
    public static final BooleanSetting PERSIST_DHT = 
        FACTORY.createBooleanSetting("PERSIST_DHT", true);
    
    /**
     * Probabilistic logic for whether or not the node should join the DHT
     * (used for initial bootstrapping)
     */
    public static final FloatSetting DHT_ACCEPT_PROBABILITY = 
        FACTORY.createSettableFloatSetting("DHT_ACCEPT_PROBABILITY", 1F, "DHTSettings.DHTAcceptProbability", 0F, 1F);
    
    /**
     * Setting for the delay between DHT node fetcher runs
     */
    public static final LongSetting DHT_NODE_FETCHER_TIME =
        //30 minutes for now
        FACTORY.createSettableLongSetting("DHT_NODE_FETCHER_TIME", 
                30L * 60L * 1000L, "DHTSettings.DHTNodeFetcherTime", 0L, 60L * 60L * 1000L); 
    
    /**
     * The maximum amount of time for which we will ping the network for DHT nodes
     */
    public static final LongSetting MAX_NODE_FETCHER_TIME = 
        FACTORY.createSettableLongSetting("MAX_NODE_FETCHER_TIME", 30L * 1000L, 
                "DHTSettings.MaxNodeFetcherTime", 0L, 5L * 60L * 1000L);
    
    /**
     * Setting for the delay between DHT random node adder runs
     */
    public static final LongSetting DHT_NODE_ADDER =
        //30 minutes for now
        FACTORY.createSettableLongSetting("DHT_NODE_ADDER_TIME", 
                30L * 60L * 1000L, "DHTSettings.DHTNodeAdderTime", 0L, 24L * 60L * 60L * 1000L); 
    
    /**
     * Setting for the number of persisted DHT nodes if this node is a passive DHT node
     * (it will not persist the entire RT)
     */
    public static final IntSetting NUM_PERSISTED_NODES = 
        FACTORY.createIntSetting("MAX_PERSISTED_NODES", 20);
}
