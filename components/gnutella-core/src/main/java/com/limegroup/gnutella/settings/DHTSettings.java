package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

import com.limegroup.gnutella.dht.DHTManager.DHTMode;

/**
 * Mojito DHT related settings.
 */
public class DHTSettings extends LimeProps {

    private DHTSettings() {}
    
    /**
     * Setting for wether or not the DHT should be active at all.
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    // Was DHTSettings.DisableDHT
    public static final BooleanSetting DISABLE_DHT_NETWORK 
        = FACTORY.createRemoteBooleanSetting("DISABLE_DHT_NETWORK", 
                true, "disable_dht_network");
    
    /**
     * Setting for wether or not the DHT should be active at all.
     */
    public static final BooleanSetting DISABLE_DHT_USER 
        = FACTORY.createBooleanSetting("DISABLE_DHT_USER", false); 
    
    /**
     * Setting to force DHT capability
     * WARNING: FOR TESTING ONLY -- DO NOT CHANGE
     */
    // Was DHTSettings.ForceDHTConnect
    public static final BooleanSetting FORCE_DHT_CONNECT 
        = FACTORY.createRemoteBooleanSetting("FORCE_DHT_CONNECT", 
                false, "force_dht_connect");
    
    /**
     * A settable list of bootstrap hosts in the host:port format
     */
    public static final StringArraySetting DHT_BOOTSTRAP_HOSTS 
        = FACTORY.createRemoteStringArraySetting("DHT_BOOTSTRAP_HOSTS", 
                new String[0], "dht_bootstrap_hosts");

    /**
     * 
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    public static final IntSetting ROUTETABLE_VERSION
        = FACTORY.createRemoteIntSetting("ROUTETABLE_VERSION", 
                0, "routetable_version", 0, Integer.MAX_VALUE);

    /**
     * Setting for the minimum average uptime (in ms) required to join the DHT.
     * WARNING: DO NOT MANUALLY CHANGE THIS 
     */
    // Was DHTSettings.MinDHTAvgUptime
    public static final LongSetting MIN_DHT_AVERAGE_UPTIME 
        = FACTORY.createRemoteLongSetting("MIN_DHT_AVERAGE_UPTIME", 120L*60L*1000L,
                "min_dht_average_uptime", 5L*60L*1000L, 48L*60L*60L*1000L);
    
    /**
     * The minimum current uptime (in ms) that a node must have to join the DHT.
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    // Was DHTSettings.MinDHTInitialUptime
    public static final LongSetting MIN_DHT_INITIAL_UPTIME 
        = FACTORY.createRemoteLongSetting("MIN_DHT_INITIAL_UPTIME", 120L*60L*1000L,
                "min_dht_initial_uptime", 5L*60L*1000L, 48L*60L*60L*1000L);
    
    /**
     * Setting for whether or not an ultrapeer can join the DHT in active mode.
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    public static final BooleanSetting EXCLUDE_ULTRAPEERS 
        = FACTORY.createBooleanSetting("EXCLUDE_ULTRAPEERS", true);
    
    /**
     * Setting for the probability to switch from DHT node to Ultrapeer node
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    // Was DHTSettings.DHTToUltrapeerProbability
    public static final FloatSetting DHT_TO_ULTRAPEER_PROBABILITY 
        = FACTORY.createRemoteFloatSetting("DHT_TO_ULTRAPEER_PROBABILITY", 
                0.5F, "dht_to_ultrapeer_probability", 0F, 1F);
    
    /**
     * Setting for whether or not the DHT should be persisted on disk
     */
    public static final BooleanSetting PERSIST_DHT 
        = FACTORY.createBooleanSetting("PERSIST_DHT", true);
    
    /**
     * Probabilistic logic for whether or not the node should join the DHT
     * (used for initial bootstrapping)
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    // Was DHTSettings.DHTAcceptProbability
    public static final FloatSetting DHT_ACCEPT_PROBABILITY 
        = FACTORY.createRemoteFloatSetting("DHT_ACCEPT_PROBABILITY", 
                1F, "dht_accept_probability", 0F, 1F);
    
    /**
     * Setting for the delay between DHT node fetcher runs
     */
    // Was DHTSettings.DHTNodeFetcherTime
    //30 minutes for now
    public static final LongSetting DHT_NODE_FETCHER_TIME 
        = FACTORY.createRemoteLongSetting("DHT_NODE_FETCHER_TIME", 
                30L*60L*1000L, "dht_node_fetcher_time", 60L*1000L, 60L*60L*1000L); 
    
    /**
     * The maximum amount of time for which we will ping the network for DHT nodes
     */
    // Was DHTSettings.MaxNodeFetcherTime
    public static final LongSetting MAX_DHT_NODE_FETCHER_TIME 
        = FACTORY.createRemoteLongSetting("MAX_DHT_NODE_FETCHER_TIME", 30L*1000L, 
                "max_dht_node_fetcher_time", 0L, 5L*60L*1000L);
    
    /**
     * Setting for the delay between DHT random node adder runs
     */
    // Was DHTSettings.DHTNodeAdderDelay
    // 30 Minutes for now
    public static final LongSetting DHT_NODE_ADDER_DELAY
        = FACTORY.createRemoteLongSetting("DHT_NODE_ADDER_DELAY", 
                30L*60L*1000L, "dht_node_adder_delay", 30L*1000L, 24L*60L*60L*1000L);
    
    /**
     * Setting for the number of persisted DHT nodes if this node is a passive DHT node
     * (it will not persist the entire RT)
     */
    public static final IntSetting MAX_PERSISTED_NODES 
        = FACTORY.createIntSetting("MAX_PERSISTED_NODES", 40);
    
    /**
     * Setting for the time at which point a file is considered rare
     * 
     * TODO: Set default and min value
     */
    public static final LongSetting RARE_FILE_TIME
        = FACTORY.createRemoteLongSetting("RARE_FILE_TIME", 
                0L, "rare_file_time", 0L, 24L*60L*60L*1000L);
    
    /**
     * Whether or not DHT querying is enabled 
     */
    public static final BooleanSetting ENABLE_DHT_QUERIES
        = FACTORY.createRemoteBooleanSetting("ENABLE_DHT_QUERIES", 
                false, "enable_dht_queries");
    
    /**
     * The maximum number of DHT requery attempts
     */
    public static final IntSetting MAX_DHT_QUERY_ATTEMPTS
        = FACTORY.createRemoteIntSetting("MAX_DHT_QUERY_ATTEMPTS", 
                1, "max_dht_query_attempts", 1, Integer.MAX_VALUE);
    
    /**
     * The minimum time between two DHT requeries
     */
    public static final LongSetting TIME_BETWEEN_DHT_QUERIES
        = FACTORY.createRemoteLongSetting("TIME_BETWEEN_DHT_QUERIES", 
                5L*60L*1000L, "time_between_dht_queries", 30L*1000L, 24L*60L*60L*1000L);
    
    /**
     * Setting for whether or not the passive DHT mode should be active at all.
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    public static final BooleanSetting ENABLE_PASSIVE_DHT_MODE
        = FACTORY.createRemoteBooleanSetting("ENABLE_PASSIVE_DHT_MODE", 
                false, "enable_passive_dht_mode");
    
    /**
     * Setting for whether or not the passive leaf DHT mode should be active at all.
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    public static final BooleanSetting ENABLE_PASSIVE_DHT_LEAF_MODE
        = FACTORY.createRemoteBooleanSetting("ENABLE_PASSIVE_DHT_LEAF_MODE", 
                false, "enable_passive_dht_leaf_mode");
    
    /**
     * Whether or not AlternativeLocations should be published.
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    public static final BooleanSetting PUBLISH_ALT_LOCS
        = FACTORY.createRemoteBooleanSetting("PUBLISH_ALT_LOCS", 
                false, "publish_alt_locs");
    
    /**
     * Whether or not PushProxies should be published.
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    public static final BooleanSetting PUBLISH_PUSH_PROXIES
        = FACTORY.createRemoteBooleanSetting("PUBLISH_PUSH_PROXIES", 
                false, "publish_push_proxies");
    
    /**
     * This setting is storing the most recent DHT mode for debugging purposes.
     * The setting is actually never read!
     */
    public static final StringSetting DHT_MODE
        = FACTORY.createStringSetting("DHT_MODE", DHTMode.INACTIVE.toString());
}
