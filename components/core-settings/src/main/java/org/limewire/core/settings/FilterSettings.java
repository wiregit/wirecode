package org.limewire.core.settings;

import org.limewire.inspection.InspectionPoint;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FloatSetting;
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
     * An array of base32-encoded hashes of spam templates.
     */
    public static final StringArraySetting SPAM_TEMPLATES =
        FACTORY.createRemoteStringArraySetting("SPAM_TEMPLATES",
                new String[0], "FilterSettings.spamTemplates");

    /**
     * An array of approximate sizes of spam files.
     */
    public static final StringArraySetting SPAM_SIZES =
        FACTORY.createRemoteStringArraySetting("SPAM_SIZES",
                new String[0], "FilterSettings.spamSizes");

    /**
     * Sets whether or not duplicate pings and queries are filtered.
     */
    public static final BooleanSetting FILTER_DUPLICATES =
        FACTORY.createBooleanSetting("FILTER_DUPLICATES", true);
    
    /**
     * The size of the <code>RepetitiveQueryFilter</code>: higher values make
     * the filter more sensitive, 0 disables it entirely.
     */
    public static final IntSetting REPETITIVE_QUERY_FILTER_SIZE =
        FACTORY.createRemoteIntSetting("REPETITIVE_QUERY_FILTER_SIZE", 10,
                "FilterSettings.repetitiveQueryFilterSize", 0, 100);

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
                new String[]{".asf", ".asx", ".au", ".htm", ".html", ".mht", ".vbs",
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
                "PCOF3DWRBLBTACCFX6UIZVGRBX7SMMYWALNGITKJWO7Z73VBB55TT2HRUKTIALGCAIMNX2QYQBTRYM46NKCUMR3XXDX6GFZETUYVUT6FGJKE2JLR5QLOJQMANPMQAC2ZUTIW56BUIB4C5ABV3OG7PUI4G3WS7R2IFTGACVOCDV5U4XDOMPJDNWKDMG4YIZDFK5Z4AWFLMSFEOLU3BVJGS5UUYLR4625UVVPVM2SNOE",
                "FilterSettings.DangerousFileTypes");

    /**
     * Base32-encoded, deflated, bencoded description of mime types.
     * See MimeTypeEncoder.
     */
    public static final StringSetting MIME_TYPES =
        FACTORY.createRemoteStringSetting("MIME_TYPES",
                "PCOK2TZ3J3BUAEHVCW4ECKV2ASX337UNNBZAAKSSIQ2NDYA54MK3HXWFL2J6IEQ4QOBJFA4QHK3WCEMWSARJO2FE2GNTP36NI4ETALAKIDTVRV7IWXWW4DSLQP747ZPQVIK5SUFRG22MJIMZUEJHDYKUO2QAHVJEYAZOXGFUZBJ2EX2UEQ7UQBSUMXKQIZI6Y7MSJECVHJU77EROJKRDBTJRBMA2UBHMHI5XMYMBQT22RNG56RMD7L7WA3UVEUAGCMPHPRVFXOA7Z2CW4EYOEHA2P52C6PUD2Z5QOM2EPAZ357Q436NNHVP3HH5IQHV7XCCDN4RBLQ2NWJUWOC535XYSE5YPOICS3C2HUWFY3Y7PK2AW4H7G6USXTJ3Q",
                "FilterSettings.MimeTypes");

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
     *
     * !!! NOTE: 5.4 and before have 'FilterSettings.urnBlacklistUpdateUrls' as their SIMPP key!!!
     *
     */
    public static final StringArraySetting URN_BLACKLIST_UPDATE_URLS =
        FACTORY.createRemoteStringArraySetting("URN_BLACKLIST_UPDATE_URLS",
                new String[]{"http://static.list.limewire.com/list/2"}, "FilterSettings.urnBlacklistUpdateUrls30KMax");

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
     * Minimum number of responses to check for similar alts.
     */
    public static final IntSetting SAME_ALTS_MIN_RESPONSES =
        FACTORY.createRemoteIntSetting("SAME_ALTS_MIN_RESPONSES", 2,
                "FilterSettings.sameAltsMinResponses", 2, 100);

    /**
     * Minimum number of alts per response to check for similar alts.
     */
    public static final IntSetting SAME_ALTS_MIN_ALTS =
        FACTORY.createRemoteIntSetting("SAME_ALTS_MIN_ALTS", 1,
                "FilterSettings.sameAltsMinAlts", 1, 100);

    /**
     * Minimum fraction of alts that must overlap for a reply to be dropped.
     */
    public static final FloatSetting SAME_ALTS_MIN_OVERLAP =
        FACTORY.createRemoteFloatSetting("SAME_ALTS_MIN_OVERLAP", 0.5f,
                "FilterSettings.sameAltsMinOverlap", 0.1f, 1f);

    /**
     * Whether replies in which all the responses have similar alts should be
     * marked as spam.
     */
    public static final BooleanSetting SAME_ALTS_ARE_SPAM =
        FACTORY.createRemoteBooleanSetting("SAME_ALTS_ARE_SPAM", true,
        "FilterSettings.sameAltsAreSpam");    

    /** Whether to enable the ClientGuidFilter. */
    public static final BooleanSetting CLIENT_GUID_FILTER =
        FACTORY.createRemoteBooleanSetting("CLIENT_GUID_FILTER", true,
        "FilterSettings.clientGuidFilter");
}
