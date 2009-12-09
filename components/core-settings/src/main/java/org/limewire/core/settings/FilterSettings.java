package org.limewire.core.settings;

import org.limewire.inspection.InspectionPoint;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

/**.
 * Settings for filters
 */
public class FilterSettings extends LimeProps {
    
    private FilterSettings() {}

    public static final BooleanSetting USE_NETWORK_FILTER =
        FACTORY.createBooleanSetting("USE_NETWORK_FILTER", true);
    
    /**
     * Sets whether or not search results including "adult content" are
     * banned in What's New queries.
     */
    public static final BooleanSetting FILTER_WHATS_NEW_ADULT =
        FACTORY.createBooleanSetting("FILTER_WHATS_NEW_ADULT", true);
    
    /**
     * Sets whether or not search results including "adult content" are
     * banned.
     */
    public static final BooleanSetting FILTER_ADULT =
        FACTORY.createBooleanSetting("FILTER_ADULT", false);
    
    /**
     * Sets whether or not known spam and malware URNs are banned.
     */
    public static final BooleanSetting FILTER_URNS =
        FACTORY.createBooleanSetting("FILTER_URNS", true);
    
    /**
     * An array of URNs that should not be displayed (local setting).
     */
    public static final StringArraySetting FILTERED_URNS_LOCAL = 
        FACTORY.createStringArraySetting("FILTERED_URNS_LOCAL", new String[0]);
    
    /**
     * An array of URNs that should not be displayed (remote setting).
     */
    public static final StringArraySetting FILTERED_URNS_REMOTE =
        FACTORY.createRemoteStringArraySetting("FILTERED_URNS_REMOTE",
                new String[0], "FilterSettings.filteredUrnsRemote");
    
    /**
     * Sets whether or not results with filtered URNs are considered spam. 
     */
    public static final BooleanSetting FILTERED_URNS_ARE_SPAM =
        FACTORY.createRemoteBooleanSetting("FILTERED_URNS_ARE_SPAM", true,
                "FilterSettings.filteredUrnsAreSpam");
    /**
     * Sets whether or not duplicate search results are
     * banned.
     */
    public static final BooleanSetting FILTER_DUPLICATES =
        FACTORY.createBooleanSetting("FILTER_DUPLICATES", true);
    
    /**
     * Sets whether or not greedy queries are filtered.
     */
    public static final BooleanSetting FILTER_GREEDY_QUERIES =
        FACTORY.createBooleanSetting("FILTER_GREEDY_QUERIES", true);
    
    /**
     * An array of IP addresses that the user has banned.
     */    
    @InspectionPoint("blacklisted hosts")
    public static final StringArraySetting BLACK_LISTED_IP_ADDRESSES =
        FACTORY.createStringArraySetting("BLACK_LISTED_IP_ADDRESSES", new String[0]);
    
    /**
     * An array of IP addresses that the user has allowed.
     */  
    public static final StringArraySetting WHITE_LISTED_IP_ADDRESSES =
        FACTORY.createStringArraySetting("WHITE_LISTED_IP_ADDRESSES", new String[0]);
    
    /**
     * An array of words that the user has banned from appearing in
     * search results.
     */
    public static final StringArraySetting BANNED_WORDS =
        FACTORY.createStringArraySetting("BANNED_WORDS", new String[0]);
    
    /**
     * An array of extensions that the user has banned from appearing in
     * search results.
     */
    public static final StringArraySetting BANNED_EXTENSIONS =
        FACTORY.createStringArraySetting("BANNED_EXTENSIONS",
                new String[]{".asf", ".asx", ".au", ".htm", ".html", ".vbs",
                ".wax", ".wm", ".wma", ".wmd", ".wmv", ".wmx", ".wmz", ".wvx"});
    
    /**
     * Whether to filter queries containing hashes.
     */ 
     /* TODO: naming convention for SIMPP keys?
     */
    public static final BooleanSetting FILTER_HASH_QUERIES =
        FACTORY.createRemoteBooleanSetting("FILTER_HASH_QUERIES", false,"filter_hash");
    
    public static final IntSetting MIN_MATCHING_WORDS =
        FACTORY.createRemoteIntSetting("MIN_MATCHING_WORDS",0,
                "FilterSettings.minMatchingWords", 0, 30);
    
    /**
     * An array of IP addresses that LimeWire will respond to.  
     */
    public static final StringArraySetting CRAWLER_IP_ADDRESSES =
        FACTORY.createRemoteStringArraySetting("CRAWLER_IPS", new String[]{"*.*.*.*"}, 
                "FilterSettings.crawlerIps");
    
    /**
     * An array of IP addresses that LimeWire will respond to with
     * inspection responses.  
     */
    public static final StringArraySetting INSPECTOR_IP_ADDRESSES =
        FACTORY.createRemoteStringArraySetting("INSPECTOR_IPS", new String[0], 
        "FilterSettings.inspectorIps");
    
    /**
     * An array of hostile IP addresses.   
     */
    public static final StringArraySetting HOSTILE_IPS =
        FACTORY.createRemoteStringArraySetting("HOSTILE_IPS", new String[0], 
        "FilterSettings.hostileIps");
    
    /**
     * How many alts to allow per response.
     */
    public static final IntSetting MAX_ALTS_PER_RESPONSE =
        FACTORY.createRemoteIntSetting("MAX_ALTS_PER_RESPONSE", 50,
                "FilterSettings.maxAltsPerResponse", 10, 100);

    /**
     * How many responses to allow per QueryReply message.
     */
    public static final IntSetting MAX_RESPONSES_PER_REPLY =
        FACTORY.createRemoteIntSetting("MAX_RESPONSES_PER_REPLY", 10, 
                "FilterSettings.maxResponsesPerReply", 10, 256);
    
    /**
     * Base32-encoded, deflated, bencoded description of dangerous file types.
     * See DangerousFileTypeEncoder.
     */
    public static final StringSetting DANGEROUS_FILE_TYPES =
        FACTORY.createRemoteStringSetting("DANGEROUS_FILE_TYPES",
                "PCOBLDTBBLBTACCGJ5KENYQYYPNWI2RBSDKKYKNJXP67JT2DH4PSUJ4U2YAMXXADAZ4BPS3YO4XXUCNMIDSMWJCE5IMYX55E53IPBCDDAGPS5CQJ4CXAPIESFOJU5R3PALBPWBJYVLP7QXRT4DJ7KCIYRMSOYLJ5OFKRM7OY23T6N37EQRSYYVVJLRKQ7D2ZU7XSPEJ7SGSD34I",
                "FilterSettings.DangerousFileTypes");

    private final static long ONE_HOUR = 60 * 60 * 1000;
    private final static long ONE_DAY = 24 * ONE_HOUR;

    /**
     * The minimum interval in milliseconds between checking for updates to the
     * URN blacklist.
     */
    public static final LongSetting MIN_URN_BLACKLIST_UPDATE_INTERVAL =
        FACTORY.createRemoteLongSetting("MIN_URN_BLACKLIST_UPDATE_INTERVAL",
                ONE_DAY, "FilterSettings.minUrnBlacklistUpdateInterval",
                ONE_HOUR, 7 * ONE_DAY);

    /**
     * The maximum interval in milliseconds between checking for updates to the
     * URN blacklist.
     */
    public static final LongSetting MAX_URN_BLACKLIST_UPDATE_INTERVAL =
        FACTORY.createRemoteLongSetting("MAX_URN_BLACKLIST_UPDATE_INTERVAL",
                28 * ONE_DAY, "FilterSettings.maxUrnBlacklistUpdateInterval",
                7 * ONE_DAY, 365 * ONE_DAY);

    /**
     * The URLs to check for URN blacklist updates.
     */
    public static final StringArraySetting URN_BLACKLIST_UPDATE_URLS =
        FACTORY.createRemoteStringArraySetting("URN_BLACKLIST_UPDATE_URLS",
                new String[0], "FilterSettings.urnBlacklistUpdateUrls");

    /**
     * The local time of the last check for URN blacklist updates.
     */
    public static final LongSetting LAST_URN_BLACKLIST_UPDATE =
        FACTORY.createLongSetting("LAST_URN_BLACKLIST_UPDATE", 0L);

    /**
     * The local time of the next check for URN blacklist updates (the check
     * will be performed at the first launch after this time).
     */
    public static final LongSetting NEXT_URN_BLACKLIST_UPDATE =
        FACTORY.createLongSetting("NEXT_URN_BLACKLIST_UPDATE", 0L);
    
    /**
     * Whether to enable the virus scanner.
     */
    public static final BooleanSetting USE_VIRUS_SCANNER =
        FACTORY.createBooleanSetting("USE_VIRUS_SCANNER", true);
    
    /**
     * Whether to enable the virus scanner for testing versions.
     */
    public static final BooleanSetting TEST_VIRUS_SCANNER =
        FACTORY.createBooleanSetting("TEST_VIRUS_SCANNER", false);
    
    /**
     * Whether to check for virus definition updates. This is set to false if
     * a virus definition update is released that requires a newer version of
     * the virus scanner library than the one that is installed, so it should
     * be reset to true if the library is updated.
     */
    public static final BooleanSetting CHECK_FOR_VIRUS_DEFINITION_UPDATES =
        FACTORY.createBooleanSetting("CHECK_FOR_VIRUS_DEFINITION_UPDATES", true);
}
