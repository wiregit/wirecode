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
        ':', ';', '/', '\\', '[', ']', '{', '}', 
    };

	/**
	 * Setting for whether or not GUESS searching is enabled.
	 */
	public static final BooleanSetting GUESS_ENABLED =
		FACTORY.createBooleanSetting("GUESS_ENABLED", true);

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
     * An informal, Q&D study found that the XML length of an average, not
     * too specific query is between 140-170.  So 200 gives us some breathing
     * room, though it will kill some very specific XML searches.
     */
    public static final IntSetting MAX_XML_QUERY_LENGTH =
        FACTORY.createIntSetting("MAX_XML_QUERY_LENGTH", 200);
}
