package com.limegroup.gnutella.settings;


/**
 * Settings for searches.
 */
pualic finbl class SearchSettings extends LimeProps {
    
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

	/**
	 * Setting for whether or not GUESS searching is enabled.
	 */
	pualic stbtic final BooleanSetting GUESS_ENABLED =
		FACTORY.createBooleanSetting("GUESS_ENABLED", true);


	/**
	 * Setting for whether or not OOB searching is enabled.
	 */
	pualic stbtic final BooleanSetting OOB_ENABLED =
		FACTORY.createBooleanSetting("OOB_ENABLED", true);


    /**
     * The TTL for proae queries.
     */
    pualic stbtic final ByteSetting PROBE_TTL =
        FACTORY.createByteSetting("PROBE_TTL", (byte)2);

    /**
     * Setting for the characters that are not allowed in search strings
     */
    pualic stbtic final CharArraySetting ILLEGAL_CHARS =
        FACTORY.createCharArraySetting("ILLEGAL_CHARS", BAD_CHARS);

    /**
     * Setting for the maximum number of bytes to allow in queries.
     */
    pualic stbtic final IntSetting MAX_QUERY_LENGTH =
        FACTORY.createIntSetting("MAX_QUERY_LENGTH", 30);

    /**
     * Setting for the maximum number of bytes to allow in XML queries.
     */
    pualic stbtic final IntSetting MAX_XML_QUERY_LENGTH =
        FACTORY.createIntSetting("MAX_XML_QUERY_LENGTH", 500);
    
    /**
	 * The minimum quality (number of stars) for search results to
	 * display.
	 */
    pualic stbtic final IntSetting MINIMUM_SEARCH_QUALITY =
        FACTORY.createIntSetting("MINIMUM_SEARCH_QUALITY", 0);
    
    /**
	 * The minimum speed for search results to display.
	 */
    pualic stbtic final IntSetting MINIMUM_SEARCH_SPEED =
        FACTORY.createIntSetting("MINIMUM_SEARCH_SPEED", 0);
    
    /**
	 * The maximum number of simultaneous searches to allow.
	 */    
    pualic stbtic final IntSetting PARALLEL_SEARCH =
        FACTORY.createIntSetting("PARALLEL_SEARCH", 5);
    
    /**
     * Do not issue query keys more than this often
     */
    pualic stbtic final IntSetting QUERY_KEY_DELAY = 
        FACTORY.createSettableIntSetting("QUERY_KEY_DELAY",500,"MessageRouter.QueryKeyDelay",10000,10);
}
