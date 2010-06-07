package org.limewire.core.settings;

import java.util.concurrent.TimeUnit;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;
import org.limewire.setting.TimeSetting;

import com.limegroup.gnutella.PushEndpoint;

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
    public static final TimeSetting MIN_ACTIVE_DHT_AVERAGE_UPTIME 
        = FACTORY.createRemoteTimeSetting("MIN_ACTIVE_DHT_AVERAGE_UPTIME", 
                2L, TimeUnit.HOURS,
                "DHT.MinActiveAverageUptime", 
                5L, TimeUnit.MINUTES,
                2L, TimeUnit.DAYS);
    
    /**
     * The minimum current uptime (in ms) that a node must have to join the DHT
     * as an ACTIVE node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    // Was DHT.MinDHTInitialUptime
    public static final TimeSetting MIN_ACTIVE_DHT_INITIAL_UPTIME 
        = FACTORY.createRemoteTimeSetting("MIN_ACTIVE_DHT_INITIAL_UPTIME", 
                2L, TimeUnit.HOURS,
                "DHT.MinActiveInitialUptime", 
                5L, TimeUnit.MINUTES, 
                2L, TimeUnit.DAYS);
    
    /**
     * Setting for the minimum average uptime (in ms) required to join the DHT
     * as a PASSIVE node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final TimeSetting MIN_PASSIVE_DHT_AVERAGE_UPTIME 
        = FACTORY.createRemoteTimeSetting("MIN_PASSIVE_DHT_AVERAGE_UPTIME", 
                2L, TimeUnit.HOURS,
                "DHT.MinPassiveAverageUptime", 
                5L, TimeUnit.MINUTES,
                2L, TimeUnit.DAYS);
    
    /**
     * The minimum current uptime (in ms) that a node must have to join the DHT
     * as a PASSIVE node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final TimeSetting MIN_PASSIVE_DHT_INITIAL_UPTIME 
        = FACTORY.createRemoteTimeSetting("MIN_PASSIVE_DHT_INITIAL_UPTIME", 
                2L, TimeUnit.HOURS,
                "DHT.MinPassiveInitialUptime", 
                5L, TimeUnit.MINUTES,
                2L, TimeUnit.DAYS);
    
    /**
     * Setting for the minimum average uptime (in ms) required to join the DHT
     * as a PASSIVE_LEAF node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final TimeSetting MIN_PASSIVE_LEAF_DHT_AVERAGE_UPTIME 
        = FACTORY.createRemoteTimeSetting("MIN_PASSIVE_LEAF_DHT_AVERAGE_UPTIME", 
                2L, TimeUnit.HOURS,
                "DHT.MinPassiveLeafAverageUptime", 
                1L, TimeUnit.MILLISECONDS,
                2L, TimeUnit.DAYS);
    
    /**
     * The minimum current uptime (in ms) that a node must have to join the DHT
     * as a PASSIVE_LEAF node.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final TimeSetting MIN_PASSIVE_LEAF_DHT_INITIAL_UPTIME 
        = FACTORY.createRemoteTimeSetting("MIN_PASSIVE_LEAF_DHT_INITIAL_UPTIME", 
                2L, TimeUnit.HOURS,
                "DHT.MinPassiveLeafInitialUptime", 
                1L, TimeUnit.MILLISECONDS,
                2L, TimeUnit.DAYS);
    
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
    public static final TimeSetting DHT_NODE_FETCHER_TIME 
        = FACTORY.createRemoteTimeSetting("DHT_NODE_FETCHER_TIME", 
                30L, TimeUnit.MINUTES,
                "DHT.NodeFetcherTime", 
                1L, TimeUnit.MINUTES,
                1L, TimeUnit.HOURS); 
    
    /**
     * Setting for the delay between DHT random node adder runs.
     */
    // Was DHT.DHTNodeAdderDelay
    // 30 Minutes for now
    public static final TimeSetting DHT_NODE_ADDER_DELAY
        = FACTORY.createRemoteTimeSetting("DHT_NODE_ADDER_DELAY", 
                30L, TimeUnit.MINUTES, 
                "DHT.NodeAdderDelay", 
                30L, TimeUnit.SECONDS, 
                1L, TimeUnit.DAYS);
    
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
    public static final TimeSetting MAX_ELAPSED_TIME_SINCE_LAST_CONTACT
        = FACTORY.createRemoteTimeSetting("MAX_ELAPSED_TIME_SINCE_LAST_CONTACT", 
                Long.MAX_VALUE, TimeUnit.MILLISECONDS,
                "DHT.MaxElapsedTimeSinceLastContact", 
                1L, TimeUnit.HOURS, 
                Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    
    /**
     * Setting for whether or not the passive RouteTable should be persisted on disk.
     */
    public static final BooleanSetting PERSIST_PASSIVE_DHT_ROUTETABLE
        = FACTORY.createRemoteBooleanSetting("PERSIST_PASSIVE_DHT_ROUTETABLE", 
                true, "DHTSettings.PersistPassiveRouteTable");
    
    /**
     * Setting for the time at which point a file is considered rare.
     */
    public static final TimeSetting RARE_FILE_TIME
        = FACTORY.createRemoteTimeSetting("RARE_FILE_TIME", 
                3L, TimeUnit.HOURS,
                "DHT.RareFileTime", 
                10L, TimeUnit.MINUTES,
                7L, TimeUnit.DAYS);
    
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
    public static final TimeSetting TIME_BETWEEN_DHT_ALT_LOC_QUERIES
        = FACTORY.createRemoteTimeSetting("TIME_BETWEEN_DHT_ALT_LOC_QUERIES", 
                30L, TimeUnit.MINUTES, 
                "DHT.TimeBetweenAltLocQueries", 
                30L, TimeUnit.SECONDS,
                1L, TimeUnit.DAYS);
    
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
     * Whether or not we query for PushProxies.
     */
    public static final BooleanSetting ENABLE_PUSH_PROXY_QUERIES
        = FACTORY.createRemoteBooleanSetting("ENABLE_PUSH_PROXY_QUERIES", 
                false, "DHT.EnablePushProxyQueriesV2");
    
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
    
    /**
     * Whether or not we allow DHTSecureMessage
     */
    public static final BooleanSetting ALLOW_DHT_SECURE_MESSAGE
        = FACTORY.createRemoteBooleanSetting("ALLOW_DHT_SECURE_MESSAGE", 
                false, "DHTSettings.allowDHTSecureMessage");
    
    /**
     * Whether or not AlternativeLocations should be published.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS
     */
    public static final BooleanSetting PUBLISH_ALT_LOCS
        = FACTORY.createRemoteBooleanSetting("PUBLISH_ALT_LOCS", 
                false, "DHT.PublishAltLocs");
    
    /**
     * The frequency at which the location publisher is running.
     */
    public static final TimeSetting LOCATION_PUBLISHER_FREQUENCY
        = FACTORY.createRemoteTimeSetting("LOCATION_PUBLISHER_FREQUENCY", 
                10L, TimeUnit.MINUTES,
                "DHT.LocationPublisherFrequency", 
                3L, TimeUnit.MINUTES, 1L, TimeUnit.DAYS);
    
    /**
     * The frequency at which locations are being published.
     */
    public static final TimeSetting PUBLISH_LOCATION_EVERY
        = FACTORY.createRemoteTimeSetting("PUBLISH_LOCATION_EVERY", 
                30L, TimeUnit.MINUTES,
                "DHT.publishLocationEvery", 
                3L, TimeUnit.MINUTES, 1L, TimeUnit.DAYS);
    
    /**
     * Whether or not PushProxies should be published.
     * <p>
     * WARNING: DO NOT MANUALLY CHANGE THIS.
     */
    public static final BooleanSetting PUBLISH_PUSH_PROXIES
        = FACTORY.createRemoteBooleanSetting("PUBLISH_PUSH_PROXIES", 
                false, "DHT.PublishPushProxies");
    
    /**
     * The frequency at which the Push-Proxy publisher is running.
     */
    public static final TimeSetting PROXY_PUBLISHER_FREQUENCY
        = FACTORY.createRemoteTimeSetting("PROXY_PUBLISHER_FREQUENCY", 
                2L, TimeUnit.MINUTES,
                "DHT.ProxyPublisherFrequency", 
                30L, TimeUnit.SECONDS, 
                1L, TimeUnit.DAYS);
    
    /**
     * The amount of time a Push-Proxy configuration must remain stable
     * (i.e. not change) before it's considered to be published to the DHT.
     */
    public static final TimeSetting STABLE_PROXIES_TIME
        = FACTORY.createRemoteTimeSetting("STABLE_PROXIES_TIME", 
                2L, TimeUnit.MINUTES,
                "DHT.StableProxiesTime", 
                1L, TimeUnit.MINUTES, 
                Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    
    /**
     * The amount of time in which stable Push-Proxy configurations are 
     * being re-published.
     */
    public static final TimeSetting PUBLISH_PROXIES_TIME
        = FACTORY.createRemoteTimeSetting("PUBLISH_PROXIES_TIME",
                30L, TimeUnit.MINUTES, 
                "DHT.PublishProxiesTime",
                10L, TimeUnit.SECONDS, 
                Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    
    /**
     * A threshold that makes sure that a client has always 
     * more than the given number of stable Push Proxies
     * published to the DHT. 
     */
    public static final IntSetting PROXY_CHANGE_THRESHOLD
        = FACTORY.createRemoteIntSetting("PROXY_CHANGE_THRESHOLD", 
                2, "DHT.ProxyChangeThreshold", 1, 32);
    
    /**
     * The frequency at which cached {@link PushEndpoint}s are being purged.
     */
    public static final TimeSetting PUSH_ENDPOINT_PURGE_FREQUENCY
        = FACTORY.createRemoteTimeSetting("PUSH_ENDPOINT_PURGE_FREQUENCY", 
                2L, TimeUnit.MINUTES,
                "DHT.PushEndpointPurgeFrequency", 
                30L, TimeUnit.SECONDS, 
                1L, TimeUnit.DAYS);
    
    /**
     * The cache time for {@link PushEndpoint}s.
     */
    public static final TimeSetting PUSH_ENDPOINT_CACHE_TIME
        = FACTORY.createRemoteTimeSetting("PUSH_ENDPOINT_CACHE_TIME",
                5L, TimeUnit.MINUTES, 
                "DHT.PushEndpointCacheTime",
                10L, TimeUnit.SECONDS, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    
    /**
     * Whether or not we should sort the Contacts we read from disk.
     */
    public static final BooleanSetting SORT_BOOTSTRAP_CONTACTS
        = FACTORY.createRemoteBooleanSetting("SORT_BOOTSTRAP_CONTACTS", 
                false, "DHT.SortBootstrapContacts");
}
