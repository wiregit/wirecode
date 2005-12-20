package com.limegroup.gnutella.settings;


/**
 * Settings for searches.
 */
public final class SearchSettings extends LimeProps {
    
    private SearchSettings() {}
    
    /**
     * Constant for the characters that are banned from search
     * strings.
     */
    private static final char[] BAD_CHARS = {
        '_', '#', '!', '|', '?', '<', '>', '^', '(', ')', 
        ':', ';', '/', '\\', '[', ']', 
        '\t', '\n', '\r', '\f', // these cannot be last or first 'cause they're trimmed
        '{', '}',
    };

    public static final int DISPLAY_JUNK_IN_PLACE = 0;
    public static final int MOVE_JUNK_TO_BOTTOM = 1;
    public static final int HIDE_JUNK = 2;
    
	/**
	 * Setting for whether or not GUESS searching is enabled.
	 */
	public static final BooleanSetting GUESS_ENABLED =
		FACTORY.createBooleanSetting("GUESS_ENABLED", true);


	/**
	 * Setting for whether or not OOB searching is enabled.
	 */
	public static final BooleanSetting OOB_ENABLED =
		FACTORY.createBooleanSetting("OOB_ENABLED", true);


    /**
     * The TTL for probe queries.
     */
    public static final ByteSetting PROBE_TTL =
        FACTORY.createByteSetting("PROBE_TTL", (byte)2);

    /**
     * Setting for the characters that are not allowed in search strings
     */
    public static final CharArraySetting ILLEGAL_CHARS =
        FACTORY.createCharArraySetting("ILLEGAL_CHARS", BAD_CHARS);

    /**
     * Setting for the maximum number of bytes to allow in queries.
     */
    public static final IntSetting MAX_QUERY_LENGTH =
        FACTORY.createIntSetting("MAX_QUERY_LENGTH", 30);

    /**
     * Setting for the maximum number of bytes to allow in XML queries.
     */
    public static final IntSetting MAX_XML_QUERY_LENGTH =
        FACTORY.createIntSetting("MAX_XML_QUERY_LENGTH", 500);
    
    /**
	 * The minimum quality (number of stars) for search results to
	 * display.
	 */
    public static final IntSetting MINIMUM_SEARCH_QUALITY =
        FACTORY.createIntSetting("MINIMUM_SEARCH_QUALITY", 0);
    
    /**
	 * The minimum speed for search results to display.
	 */
    public static final IntSetting MINIMUM_SEARCH_SPEED =
        FACTORY.createIntSetting("MINIMUM_SEARCH_SPEED", 0);
    
    /**
	 * The maximum number of simultaneous searches to allow.
	 */    
    public static final IntSetting PARALLEL_SEARCH =
        FACTORY.createIntSetting("PARALLEL_SEARCH", 5);
	
	/**
	 * Whether or not to enable the spam filter.
	 */    
    public static final BooleanSetting ENABLE_SPAM_FILTER =
        FACTORY.createBooleanSetting("ENABLE_SPAM_FILTER", true);

    /**
     * The display mode for junk search results
     */    
    public static final IntSetting DISPLAY_JUNK_MODE =
        FACTORY.createIntSetting("DISPLAY_JUNK_MODE", MOVE_JUNK_TO_BOTTOM);
    
    public static boolean moveJunkToBottom() {
        return ENABLE_SPAM_FILTER.getValue() && DISPLAY_JUNK_MODE.getValue() == MOVE_JUNK_TO_BOTTOM;
    }
    
    public static boolean hideJunk() {
        return ENABLE_SPAM_FILTER.getValue() && DISPLAY_JUNK_MODE.getValue() == HIDE_JUNK;
    }
    
    /**
	 * Set how sensitive the spamfilter should be
	 */    
    public static final FloatSetting FILTER_SPAM_RESULTS =
        FACTORY.createFloatSetting("FILTER_SPAM_RESULTS", 0.8585858585858585f);
    
    /**
     * The minimum spam rating at which we stop counting results for 
     * dynamic querying.  Meant to prevent very strict user settings
     * from making dynamic querying too agressive. 
     */
    public static final FloatSetting QUERY_SPAM_CUTOFF =
        FACTORY.createSettableFloatSetting("QUERY_SPAM_CUTOFF",0.4f,
                "SpamManager.displayTreshold",1.0f,0.1f);

    /**
     * The percentage of normal results that spam results bring to
     * the dynamic querying mechanism
     */
    public static final FloatSetting SPAM_RESULT_RATIO =
	FACTORY.createSettableFloatSetting("SPAM_RESULT_RATIO", 0.3f,
		"SpamManager.resultRatio",1.0f,0.2f);
    
	/**
     * Do not issue query keys more than this often
     */
    public static final IntSetting QUERY_KEY_DELAY = 
        FACTORY.createSettableIntSetting("QUERY_KEY_DELAY",500,
                "MessageRouter.QueryKeyDelay",10000,10);
}
