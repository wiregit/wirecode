package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.StringArraySetting;

/**
 * Mojito DHT related settings.
 * 
 */
public class DHTSettings extends LimeProps{

    private DHTSettings() {}
    
    /**
     * A settable list of bootstrap hosts in the host:port format
     */
    public static final StringArraySetting DHT_BOOTSTRAP_HOSTS =
        FACTORY.createRemoteStringArraySetting("DHT_BOOTSTRAP_HOSTS", new String[0], "dht_bootstrap_hosts");

    /**
     * Setting for whether or not we are active DHT capable.
     * WARNING : DO NOT MANUALLY CHANGE THIS
     */
    public static final BooleanSetting ACTIVE_DHT_CAPABLE =
        FACTORY.createExpirableBooleanSetting("ACTIVE_DHT_CAPABLE", false);
    
    /**
     * Setting to force DHT capability
     * WARNING : FOR TESTING ONLY -- DO NOT CHANGE
     */
    public static final BooleanSetting FORCE_DHT_CONNECT =
    	FACTORY.createRemoteBooleanSetting("FORCE_DHT_CONNECT", false, "DHTSettings.ForceDHTConnect");

    /**
     * Setting for wether or not the DHT should be active at all.
     */
    public static final BooleanSetting DISABLE_DHT_USER =
        FACTORY.createBooleanSetting("DISABLE_DHT_USER", false);

    /**
     * Setting for wether or not the DHT should be active at all.
     * WARNING : DO NOT MANUALLY CHANGE THIS
     */
    public static final BooleanSetting DISABLE_DHT_NETWORK =
        FACTORY.createRemoteBooleanSetting("DISABLE_DHT_NETWORK", true, "DHTSettings.DisableDHT");
    
    /**
     * Setting for whether or not the passive DHT mode should be active at all.
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    public static final BooleanSetting DISABLE_PASSIVE_DHT
        = FACTORY.createRemoteBooleanSetting("DISABLE_PASSIVE_DHT", false, "disable_passive_dht");
    
    /**
     * Setting for the minimum average uptime (in ms) required to join the DHT.
     * WARNING : DO NOT MANUALLY CHANGE THIS 
     */
    public static final LongSetting MIN_DHT_AVG_UPTIME =
        FACTORY.createRemoteLongSetting("MIN_DHT_AVG_UPTIME", 120L*60L*1000L,
                "DHTSettings.MinDHTAvgUptime", 5L*60L*1000L, 48L*3600L*1000L);
    
    /**
     * The minimum current uptime (in ms) that a node must have to join the DHT.
     * WARNING : DO NOT MANUALLY CHANGE THIS
     */
    public static final LongSetting MIN_DHT_INITIAL_UPTIME =
        FACTORY.createRemoteLongSetting("MIN_DHT_INITIAL_UPTIME", 120L*60L*1000L,
                "DHTSettings.MinDHTInitialUptime", 5L*60L*1000L, 48L*3600L*1000L);
    
    /**
     * Setting for whether or not an ultrapeer can join the DHT.
     * WARNING : DO NOT MANUALLY CHANGE THIS
     */
    public static final BooleanSetting EXCLUDE_ULTRAPEERS =
        FACTORY.createBooleanSetting("EXCLUDE_ULTRAPEERS", true);
    
    /**
     * Setting for the probability to switch from DHT node to Ultrapeer node
     * WARNING : DO NOT MANUALLY CHANGE THIS
     */
    public static final FloatSetting DHT_TO_ULTRAPEER_PROBABILITY =
        FACTORY.createRemoteFloatSetting("DHT_TO_ULTRAPEER_PROBABILITY", 0.5F, "DHTSettings.DHTToUltrapeerProbability",0F,1F);
    
    /**
     * Setting for whether or not the DHT should be persisted on disk
     */
    public static final BooleanSetting PERSIST_DHT = 
        FACTORY.createBooleanSetting("PERSIST_DHT", true);
    
    /**
     * Probabilistic logic for whether or not the node should join the DHT
     * (used for initial bootstrapping)
     * WARNING : DO NOT MANUALLY CHANGE THIS
     * 
     */
    public static final FloatSetting DHT_ACCEPT_PROBABILITY = 
        FACTORY.createRemoteFloatSetting("DHT_ACCEPT_PROBABILITY", 1F, "DHTSettings.DHTAcceptProbability", 0F, 1F);
    
    /**
     * Setting for the delay between DHT node fetcher runs
     */
    public static final LongSetting DHT_NODE_FETCHER_TIME =
        //30 minutes for now
        FACTORY.createRemoteLongSetting("DHT_NODE_FETCHER_TIME", 
                30L * 60L * 1000L, "DHTSettings.DHTNodeFetcherTime", 60L * 1000L, 60L * 60L * 1000L); 
    
    /**
     * The maximum amount of time for which we will ping the network for DHT nodes
     */
    public static final LongSetting MAX_NODE_FETCHER_TIME = 
        FACTORY.createRemoteLongSetting("MAX_NODE_FETCHER_TIME", 30L * 1000L, 
                "DHTSettings.MaxNodeFetcherTime", 0L, 5L * 60L * 1000L);
    
    /**
     * Setting for the delay between DHT random node adder runs
     */
    public static final LongSetting DHT_NODE_ADDER_DELAY =
        //30 minutes for now
        FACTORY.createRemoteLongSetting("DHT_NODE_ADDER_DELAY", 
                30L * 60L * 1000L, "DHTSettings.DHTNodeAdderDelay", 30L * 1000L, 24L * 60L * 60L * 1000L); 
    
    /**
     * Setting for the number of persisted DHT nodes if this node is a passive DHT node
     * (it will not persist the entire RT)
     */
    public static final IntSetting NUM_PERSISTED_NODES = 
        FACTORY.createIntSetting("MAX_PERSISTED_NODES", 40);
}
