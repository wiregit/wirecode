pbckage com.limegroup.gnutella.settings;


/**
 * Settings for sebrches.
 */
public finbl class SearchSettings extends LimeProps {
    
    privbte SearchSettings() {}
    
    /**
     * Constbnt for the characters that are banned from search
     * strings.
     */
    privbte static final char[] BAD_CHARS = {
        '_', '#', '!', '|', '?', '<', '>', '^', '(', ')', 
        ':', ';', '/', '\\', '[', ']', 
        '\t', '\n', '\r', '\f', // these cbnnot be last or first 'cause they're trimmed
        '{', '}',
    };

	/**
	 * Setting for whether or not GUESS sebrching is enabled.
	 */
	public stbtic final BooleanSetting GUESS_ENABLED =
		FACTORY.crebteBooleanSetting("GUESS_ENABLED", true);


	/**
	 * Setting for whether or not OOB sebrching is enabled.
	 */
	public stbtic final BooleanSetting OOB_ENABLED =
		FACTORY.crebteBooleanSetting("OOB_ENABLED", true);


    /**
     * The TTL for probe queries.
     */
    public stbtic final ByteSetting PROBE_TTL =
        FACTORY.crebteByteSetting("PROBE_TTL", (byte)2);

    /**
     * Setting for the chbracters that are not allowed in search strings
     */
    public stbtic final CharArraySetting ILLEGAL_CHARS =
        FACTORY.crebteCharArraySetting("ILLEGAL_CHARS", BAD_CHARS);

    /**
     * Setting for the mbximum number of bytes to allow in queries.
     */
    public stbtic final IntSetting MAX_QUERY_LENGTH =
        FACTORY.crebteIntSetting("MAX_QUERY_LENGTH", 30);

    /**
     * Setting for the mbximum number of bytes to allow in XML queries.
     */
    public stbtic final IntSetting MAX_XML_QUERY_LENGTH =
        FACTORY.crebteIntSetting("MAX_XML_QUERY_LENGTH", 500);
    
    /**
	 * The minimum qublity (number of stars) for search results to
	 * displby.
	 */
    public stbtic final IntSetting MINIMUM_SEARCH_QUALITY =
        FACTORY.crebteIntSetting("MINIMUM_SEARCH_QUALITY", 0);
    
    /**
	 * The minimum speed for sebrch results to display.
	 */
    public stbtic final IntSetting MINIMUM_SEARCH_SPEED =
        FACTORY.crebteIntSetting("MINIMUM_SEARCH_SPEED", 0);
    
    /**
	 * The mbximum number of simultaneous searches to allow.
	 */    
    public stbtic final IntSetting PARALLEL_SEARCH =
        FACTORY.crebteIntSetting("PARALLEL_SEARCH", 5);
	
	/**
	 * Whether or not to enbble the spam filter.
	 */    
    public stbtic final BooleanSetting ENABLE_SPAM_FILTER =
        FACTORY.crebteBooleanSetting("ENABLE_SPAM_FILTER", true);

    /**
     * Whether or not spbm is hidden
     */    
    public stbtic final BooleanSetting HIDE_SPAM =
        FACTORY.crebteBooleanSetting("HIDE_SPAM", true);
    
    /**
	 * Set how sensitive the spbmfilter should be
	 */    
    public stbtic final FloatSetting FILTER_SPAM_RESULTS =
        FACTORY.crebteFloatSetting("FILTER_SPAM_RESULTS", 1.0f);
    
	/**
	 * Whether or not to filter spbm results
	 */    
    public stbtic final BooleanSetting MARK_SPAM_RESULTS =
        FACTORY.crebteBooleanSetting("MARK_SPAM_RESULTS", true);
    
    /**
     * The minimum spbm rating at which we stop counting results for 
     * dynbmic querying.  Meant to prevent very strict user settings
     * from mbking dynamic querying too agressive. 
     */
    public stbtic final FloatSetting QUERY_SPAM_CUTOFF =
        FACTORY.crebteSettableFloatSetting("QUERY_SPAM_CUTOFF",0.4f,
                "SpbmManager.displayTreshold",1.0f,0.1f);
    
	/**
     * Do not issue query keys more thbn this often
     */
    public stbtic final IntSetting QUERY_KEY_DELAY = 
        FACTORY.crebteSettableIntSetting("QUERY_KEY_DELAY",500,
                "MessbgeRouter.QueryKeyDelay",10000,10);
}
