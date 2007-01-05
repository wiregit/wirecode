
package com.limegroup.gnutella.settings;

/**
 * Settings for filters
 */
public class FilterSettings extends LimeProps {
    
    private FilterSettings() {}

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
	 * Sets whether or not search results including VBS are
	 * banned.
	 */
    public static final BooleanSetting FILTER_VBS =
        FACTORY.createBooleanSetting("FILTER_VBS", true);
    
    /**
	 * Sets whether or not search results including HTML are
	 * banned.
	 */
    public static final BooleanSetting FILTER_HTML =
        FACTORY.createBooleanSetting("FILTER_HTML", false);
    
    /**
     * Sets whether or not search results of the wmv and asf types are banned.
     */
    public static final BooleanSetting FILTER_WMV_ASF =
    	FACTORY.createBooleanSetting("FILTER_WMV_ASF",true);
    
    /**
	 * Sets whether or not duplicate search results are
	 * banned.
	 */
    public static final BooleanSetting FILTER_DUPLICATES =
        FACTORY.createBooleanSetting("FILTER_DUPLICATES", true);
    
    /**
	 * Sets whether or not greedy queries a filtered.
	 */
    public static final BooleanSetting FILTER_GREEDY_QUERIES =
        FACTORY.createBooleanSetting("FILTER_GREEDY_QUERIES", true);
    
    /**
	 * Sets whether or not high bit queries a filtered.
	 */    
    public static final BooleanSetting FILTER_HIGHBIT_QUERIES =
        FACTORY.createBooleanSetting("FILTER_HIGHBIT_QUERIES", true);
    
    /**
	 * An array of ip addresses that the user has banned.
	 */    
    public static final StringArraySetting BLACK_LISTED_IP_ADDRESSES =
        FACTORY.createStringArraySetting("BLACK_LISTED_IP_ADDRESSES", new String[0]);
    
    /**
	 * An array of ip addresses that the user has allowed. (Array of String!)
	 */  
    public static final StringArraySetting WHITE_LISTED_IP_ADDRESSES =
        FACTORY.createStringArraySetting("WHITE_LISTED_IP_ADDRESSES", new String[0]);
    
    /**
     * An array of ip addresses that LimeWire will respond to.  
     */
    public static final StringArraySetting CRAWLER_IP_ADDRESSES =
        FACTORY.createSettableStringArraySetting("CRAWLER_IPS", new String[]{"*.*.*.*"}, 
                "FilterSettings.crawlerIps");
    
    /**
	 * An array of words that the user has banned from appearing in
	 * search results.
	 */
    public static final StringArraySetting BANNED_WORDS =
        FACTORY.createStringArraySetting("BANNED_WORDS", new String[0]);
    
    /**
     * Whether to filter queries containing hashes.
     * TODO: naming convention for SIMPP keys?
     */
    public static final BooleanSetting FILTER_HASH_QUERIES =
        FACTORY.createSettableBooleanSetting("FILTER_HASH_QUERIES",false,"filter_hash");
    
    public static final IntSetting MIN_MATCHING_WORDS =
    	FACTORY.createSettableIntSetting("MIN_MATCHING_WORDS",0,
    			"FilterSettings.minMatchingWords", 0, 30);
    
    /** 
     * Whether to drop responses that have an action 
     */
    public static final BooleanSetting FILTER_ACTION_RESPONSES =
    		FACTORY.createSettableBooleanSetting("FILTER_ACTION_RESPONSES",false,
    				"FilterSettings.filterActionResponses");
}
