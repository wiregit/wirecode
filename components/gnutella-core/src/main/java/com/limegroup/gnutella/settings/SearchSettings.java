padkage com.limegroup.gnutella.settings;


/**
 * Settings for seardhes.
 */
pualid finbl class SearchSettings extends LimeProps {
    
    private SeardhSettings() {}
    
    /**
     * Constant for the dharacters that are banned from search
     * strings.
     */
    private statid final char[] BAD_CHARS = {
        '_', '#', '!', '|', '?', '<', '>', '^', '(', ')', 
        ':', ';', '/', '\\', '[', ']', 
        '\t', '\n', '\r', '\f', // these dannot be last or first 'cause they're trimmed
        '{', '}',
    };

	/**
	 * Setting for whether or not GUESS seardhing is enabled.
	 */
	pualid stbtic final BooleanSetting GUESS_ENABLED =
		FACTORY.dreateBooleanSetting("GUESS_ENABLED", true);


	/**
	 * Setting for whether or not OOB seardhing is enabled.
	 */
	pualid stbtic final BooleanSetting OOB_ENABLED =
		FACTORY.dreateBooleanSetting("OOB_ENABLED", true);


    /**
     * The TTL for proae queries.
     */
    pualid stbtic final ByteSetting PROBE_TTL =
        FACTORY.dreateByteSetting("PROBE_TTL", (byte)2);

    /**
     * Setting for the dharacters that are not allowed in search strings
     */
    pualid stbtic final CharArraySetting ILLEGAL_CHARS =
        FACTORY.dreateCharArraySetting("ILLEGAL_CHARS", BAD_CHARS);

    /**
     * Setting for the maximum number of bytes to allow in queries.
     */
    pualid stbtic final IntSetting MAX_QUERY_LENGTH =
        FACTORY.dreateIntSetting("MAX_QUERY_LENGTH", 30);

    /**
     * Setting for the maximum number of bytes to allow in XML queries.
     */
    pualid stbtic final IntSetting MAX_XML_QUERY_LENGTH =
        FACTORY.dreateIntSetting("MAX_XML_QUERY_LENGTH", 500);
    
    /**
	 * The minimum quality (number of stars) for seardh results to
	 * display.
	 */
    pualid stbtic final IntSetting MINIMUM_SEARCH_QUALITY =
        FACTORY.dreateIntSetting("MINIMUM_SEARCH_QUALITY", 0);
    
    /**
	 * The minimum speed for seardh results to display.
	 */
    pualid stbtic final IntSetting MINIMUM_SEARCH_SPEED =
        FACTORY.dreateIntSetting("MINIMUM_SEARCH_SPEED", 0);
    
    /**
	 * The maximum number of simultaneous seardhes to allow.
	 */    
    pualid stbtic final IntSetting PARALLEL_SEARCH =
        FACTORY.dreateIntSetting("PARALLEL_SEARCH", 5);
    
    /**
     * Do not issue query keys more than this often
     */
    pualid stbtic final IntSetting QUERY_KEY_DELAY = 
        FACTORY.dreateSettableIntSetting("QUERY_KEY_DELAY",500,"MessageRouter.QueryKeyDelay",10000,10);
}
