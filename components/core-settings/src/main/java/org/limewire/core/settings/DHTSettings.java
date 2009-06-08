package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

/**
 * Mojito DHT related settings.
 */
public class DHTSettings extends LimeProps {

    private DHTSettings() {}
    
    /**
     * Setting for whether or not the DHT should be active at all.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    // Was DHTSettings.DisableDHT // 4.13.1 & 4.13.2
    public static final BooleanSetting DISABLE_DHT_NETWORK 
        = FACTORY.createRemoteBooleanSetting("DISABLE_DHT_NETWORK", 
                true, "DHT.DisableNetwork");
    
    /**
     * Setting for whether or not the DHT should be active at all.
     */
    public static final BooleanSetting DISABLE_DHT_USER 
        = FACTORY.createBooleanSetting("DISABLE_DHT_USER", false); 
    
    /**
     * Setting to force DHT capability.
     * <p>
     * WARNING: FOR TESTING ONLY -- DO NOT CHANGE.
     */
    // Was DHT.ForceDHTConnect
    public static final BooleanSetting FORCE_DHT_CONNECT 
        = FACTORY.createRemoteBooleanSetting("FORCE_DHT_CONNECT", 
                false, "DHT.ForceConnect");
    
    /**
     * A settable list of bootstrap hosts in the host:port format.
     */
    public static final StringArraySetting DHT_BOOTSTRAP_HOSTS 
        = FACTORY.createRemoteStringArraySetting("DHT_BOOTSTRAP_HOSTS", 
                new String[0], "DHT.BootstrapHosts");

    /**
     * Version of serialized RouteTable.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final IntSetting ACTIVE_DHT_ROUTETABLE_VERSION
        = FACTORY.createRemoteIntSetting("ACTIVE_DHT_ROUTETABLE_VERSION", 
                0, "DHT.ActiveRouteTableVersion", 0, Integer.MAX_VALUE);

    /**
     * Version of serialized RouteTable.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final IntSetting PASSIVE_DHT_ROUTETABLE_VERSION
        = FACTORY.createRemoteIntSetting("PASSIVE_DHT_ROUTETABLE_VERSION", 
                0, "DHT.PassiveRouteTableVersion", 0, Integer.MAX_VALUE);
    
    /**
     * Setting for the minimum average uptime (in ms) required to join the DHT
     * as an ACTIVE node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    // Was DHT.MinDHTAvgUptime
    public static final LongSetting MIN_ACTIVE_DHT_AVERAGE_UPTIME 
        = FACTORY.createRemoteLongSetting("MIN_ACTIVE_DHT_AVERAGE_UPTIME", 2L*60L*60L*1000L,
                "DHT.MinActiveAverageUptime", 5L*60L*1000L, 48L*60L*60L*1000L);
    
    /**
     * The minimum current uptime (in ms) that a node must have to join the DHT
     * as an ACTIVE node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    // Was DHT.MinDHTInitialUptime
    public static final LongSetting MIN_ACTIVE_DHT_INITIAL_UPTIME 
        = FACTORY.createRemoteLongSetting("MIN_ACTIVE_DHT_INITIAL_UPTIME", 2L*60L*60L*1000L,
                "DHT.MinActiveInitialUptime", 5L*60L*1000L, 48L*60L*60L*1000L);
    
    /**
     * Setting for the minimum average uptime (in ms) required to join the DHT
     * as a PASSIVE node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final LongSetting MIN_PASSIVE_DHT_AVERAGE_UPTIME 
        = FACTORY.createRemoteLongSetting("MIN_PASSIVE_DHT_AVERAGE_UPTIME", 2L*60L*60L*1000L,
                "DHT.MinPassiveAverageUptime", 5L*60L*1000L, 48L*60L*60L*1000L);
    
    /**
     * The minimum current uptime (in ms) that a node must have to join the DHT
     * as a PASSIVE node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final LongSetting MIN_PASSIVE_DHT_INITIAL_UPTIME 
        = FACTORY.createRemoteLongSetting("MIN_PASSIVE_DHT_INITIAL_UPTIME", 2L*60L*60L*1000L,
                "DHT.MinPassiveInitialUptime", 5L*60L*1000L, 48L*60L*60L*1000L);
    
    /**
     * Setting for the minimum average uptime (in ms) required to join the DHT
     * as a PASSIVE_LEAF node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final LongSetting MIN_PASSIVE_LEAF_DHT_AVERAGE_UPTIME 
        = FACTORY.createRemoteLongSetting("MIN_PASSIVE_LEAF_DHT_AVERAGE_UPTIME", 2L*60L*60L*1000L,
                "DHT.MinPassiveLeafAverageUptime", 1L, 48L*60L*60L*1000L);
    
    /**
     * The minimum current uptime (in ms) that a node must have to join the DHT
     * as a PASSIVE_LEAF node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final LongSetting MIN_PASSIVE_LEAF_DHT_INITIAL_UPTIME 
        = FACTORY.createRemoteLongSetting("MIN_PASSIVE_LEAF_DHT_INITIAL_UPTIME", 2L*60L*60L*1000L,
                "DHT.MinPassiveLeafInitialUptime", 1L, 48L*60L*60L*1000L);
    
    /**
     * Setting for whether or not an Ultrapeer can join the DHT in active mode.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final BooleanSetting EXCLUDE_ULTRAPEERS 
        = FACTORY.createBooleanSetting("EXCLUDE_ULTRAPEERS", true);
    
    /**
     * Setting for the probability to switch from DHT node to Ultrapeer node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    // Was DHT.DHTToUltrapeerProbability
    public static final FloatSetting SWITCH_TO_ULTRAPEER_PROBABILITY 
        = FACTORY.createRemoteFloatSetting("SWITCH_TO_ULTRAPEER_PROBABILITY", 
                0.5F, "DHT.SwitchToUltrapeerProbability", 0F, 1F);
    
    /**
     * Probabilistic logic for whether or not the node should join the DHT
     * (used for initial bootstrapping).
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    // Was DHT.DHTAcceptProbability
    public static final FloatSetting DHT_ACCEPT_PROBABILITY 
        = FACTORY.createRemoteFloatSetting("DHT_ACCEPT_PROBABILITY", 
                1F, "DHT.AcceptProbability", 0F, 1F);
    
    /**
     * Setting for the delay between DHT node fetcher runs.
     */
    // Was DHT.DHTNodeFetcherTime
    //30 minutes for now
    public static final LongSetting DHT_NODE_FETCHER_TIME 
        = FACTORY.createRemoteLongSetting("DHT_NODE_FETCHER_TIME", 
                30L*60L*1000L, "DHT.NodeFetcherTime", 60L*1000L, 60L*60L*1000L); 
    
    /**
     * The maximum amount of time for which we will ping the network for DHT nodes.
     */
    // Was DHT.MaxNodeFetcherTime
    public static final LongSetting MAX_DHT_NODE_FETCHER_TIME 
        = FACTORY.createRemoteLongSetting("MAX_DHT_NODE_FETCHER_TIME", 30L*1000L, 
                "DHT.MaxNodeFetcherTime", 0L, 5L*60L*1000L);
    
    /**
     * Setting for the delay between DHT random node adder runs.
     */
    // Was DHT.DHTNodeAdderDelay
    // 30 Minutes for now
    public static final LongSetting DHT_NODE_ADDER_DELAY
        = FACTORY.createRemoteLongSetting("DHT_NODE_ADDER_DELAY", 
                30L*60L*1000L, "DHT.NodeAdderDelay", 30L*1000L, 24L*60L*60L*1000L);
    
    /**
     * Setting for the number of persisted DHT nodes if this node is a passive DHT node
     * (it will not persist the entire RT).
     */
    public static final IntSetting MAX_PERSISTED_NODES 
        = FACTORY.createRemoteIntSetting("MAX_PERSISTED_NODES", 
                40, "DHT.MaxPersistedNodes", 0, 1024);
    
    /**
     * Setting for whether or not the RouteTable should be persisted on disk.
     */
    public static final BooleanSetting PERSIST_ACTIVE_DHT_ROUTETABLE
        = FACTORY.createRemoteBooleanSetting("PERSIST_ACTIVE_DHT_ROUTETABLE", 
                true, "DHTSettings.PersistActiveRouteTable");
    
    /**
     * Setting for whether or not the RouteTable should be purged from very
     * old Contacts. The goal is to merge Buckets and to lower their count.
     * <p>
     * Default is Long.MAX_VALUE and means purging is turned off!
     */
    public static final LongSetting MAX_ELAPSED_TIME_SINCE_LAST_CONTACT
        = FACTORY.createRemoteLongSetting("MAX_ELAPSED_TIME_SINCE_LAST_CONTACT", 
                Long.MAX_VALUE, "DHT.MaxElapsedTimeSinceLastContact", 60L*60L*1000L, Long.MAX_VALUE);
    
    /**
     * Setting for whether or not the passive RouteTable should be persisted on disk.
     */
    public static final BooleanSetting PERSIST_PASSIVE_DHT_ROUTETABLE
        = FACTORY.createRemoteBooleanSetting("PERSIST_PASSIVE_DHT_ROUTETABLE", 
                true, "DHTSettings.PersistPassiveRouteTable");
    
    /**
     * Setting for whether or not the Database should be persisted on disk.
     */
    // Default value is true in LW 4.13.8 and older
    public static final BooleanSetting PERSIST_DHT_DATABASE
        = FACTORY.createRemoteBooleanSetting("PERSIST_DHT_DATABASE", 
                false, "DHTSettings.PersistDatabase");
    
    /**
     * Setting for the time at which point a file is considered rare.
     */
    public static final LongSetting RARE_FILE_TIME
        = FACTORY.createRemoteLongSetting("RARE_FILE_TIME", 
                3L*60L*60L*1000L, "DHT.RareFileTime", 10L*60L*1000L, 7L*24L*60L*60L*1000L);
    
    /**
     * The minimum number of upload attempts before a file is considered
     * rate. This make sure we don't upload every file.
     */
    public static final IntSetting RARE_FILE_ATTEMPTED_UPLOADS
        = FACTORY.createRemoteIntSetting("RARE_FILE_ATTEMPTED_UPLOADS", 
                1, "DHT.RareFileAttemptedUploads", 0, Integer.MAX_VALUE);
    
    /**
     * The number of times a file must have been uploaded before it's considered 
     * rare. This make sure we don't upload every file.
     */
    public static final IntSetting RARE_FILE_COMPLETED_UPLOADS
        = FACTORY.createRemoteIntSetting("RARE_FILE_COMPLETED_UPLOADS", 
                0, "DHT.RareFileCompletedUploads", 0, Integer.MAX_VALUE);
    
    /**
     * Definition whether a file is rare.  For the list of keys look at
     * FileDesc.lookup(String).
     */
    public static final StringArraySetting RARE_FILE_DEFINITION =
        FACTORY.createRemoteStringArraySetting("RARE_FILE_DEFINITION", 
                new String[]{"ups","atUpSet","<","cups","cUpSet","<","OR","NOT",
                "lastup","rftSet",">","AND"}, 
                "DHT.RareFileDefinition");
    /**
     * Whether or not AlternativeLocations should be published.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    public static final BooleanSetting PUBLISH_ALT_LOCS
        = FACTORY.createRemoteBooleanSetting("PUBLISH_ALT_LOCS", 
                false, "DHT.PublishAltLocs");
    
    /**
     * Whether or not DHT querying is enabled.
     */
    public static final BooleanSetting ENABLE_DHT_ALT_LOC_QUERIES
        = FACTORY.createRemoteBooleanSetting("ENABLE_DHT_ALT_LOC_QUERIES", 
                false, "DHT.EnableAltLocQueriesV2");
    
    /**
     * The maximum number of DHT requery attempts.
     */
    public static final IntSetting MAX_DHT_ALT_LOC_QUERY_ATTEMPTS
        = FACTORY.createRemoteIntSetting("MAX_DHT_ALT_LOC_QUERY_ATTEMPTS", 
                1, "DHT.MaxAltLocQueryAttempts", 1, Integer.MAX_VALUE);
    
    /**
     * The minimum time between two DHT requeries.
     */
    public static final LongSetting TIME_BETWEEN_DHT_ALT_LOC_QUERIES
        = FACTORY.createRemoteLongSetting("TIME_BETWEEN_DHT_ALT_LOC_QUERIES", 
                30L*60L*1000L, "DHT.TimeBetweenAltLocQueries", 30L*1000L, 24L*60L*60L*1000L);
    
    /**
     * Setting for whether or not the passive DHT mode should be active at all.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    // Was DHT.EnablePassiveMode in 4.13.12 and prior. 
    // Deadlock! Do not turn on! See MOJITO-119!
    public static final BooleanSetting ENABLE_PASSIVE_DHT_MODE
        = FACTORY.createRemoteBooleanSetting("ENABLE_PASSIVE_DHT_MODE", 
                false, "DHT.EnablePassiveModeV2");
    
    /**
     * Setting for whether or not the passive leaf DHT mode should be active at all.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final BooleanSetting ENABLE_PASSIVE_LEAF_DHT_MODE
        = FACTORY.createRemoteBooleanSetting("ENABLE_PASSIVE_LEAF_DHT_MODE", 
                false, "DHT.EnablePassiveLeafMode");
    
    /**
     * Whether or not PushProxies should be published.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final BooleanSetting PUBLISH_PUSH_PROXIES
        = FACTORY.createRemoteBooleanSetting("PUBLISH_PUSH_PROXIES", 
                false, "DHT.PublishPushProxies");
    
    /**
     * Whether or not we query for PushProxies.
     */
    public static final BooleanSetting ENABLE_PUSH_PROXY_QUERIES
        = FACTORY.createRemoteBooleanSetting("ENABLE_PUSH_PROXY_QUERIES", 
                false, "DHT.EnablePushProxyQueriesV2");
    
    /**
     * Time between push proxy queries.
     */
    public static final LongSetting TIME_BETWEEN_PUSH_PROXY_QUERIES
        = FACTORY.createRemoteLongSetting("TIME_BETWEEN_PUSH_PROXY_QUERIES",
                5L * 60L * 1000L, "DHT.TimeBetweenPushProxyQueries",
                10L * 1000L, Long.MAX_VALUE);

    /**
     * The time in milliseconds push proxies have to be stable before being published.
     */
    public static final LongSetting PUSH_PROXY_STABLE_PUBLISHING_INTERVAL
    = FACTORY.createRemoteLongSetting("PUSH_PROXY_STABLE_PUBLISHING_INTERVAL",
            60L * 1000L, "DHT.pushProxyStablePublishingInterval",
            10L * 1000L, Long.MAX_VALUE);
    
    /**
     * This setting is storing the most recent DHT Node ID for debugging purposes.
     * <p>
     * The setting is actually never read!
     */
    public static final StringSetting DHT_NODE_ID
        = FACTORY.createStringSetting("DHT_NODE_ID", "");
    
    /**
     * This setting is storing the most recent DHT mode for debugging purposes.
     * <p>
     * The setting is actually never read!
     */
    public static final StringSetting DHT_MODE
        = FACTORY.createStringSetting("DHT_MODE", "");
    
}
