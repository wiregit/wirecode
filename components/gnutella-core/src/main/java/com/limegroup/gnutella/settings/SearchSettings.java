
// Commented for the Learning branch

package com.limegroup.gnutella.settings;

/**
 * Settings for searches.
 * 
 * The LimeWire GUI uses these settings to check user input and choose which search results to display.
 * In the LimeWire core, QueryRequest uses these settings to validate the parts of an incoming query packet.
 * Many of these settings deal with QueryKey and GUESS, which LimeWire no longer uses.
 */
public final class SearchSettings extends LimeProps {

    /**
     * Don't let anyone make a SearchSettings object.
     * Just use the static members here instead.
     */
    private SearchSettings() {}

    /**
     * The characters that are not allowed in search text, like _ # ! |.
     * Four of the banned characters, '\t', '\n', '\r', '\f', won't appear first or last because they get trimmed.
     * 
     * Used only by the ILLEGAL_CHARS setting below.
     */
    private static final char[] BAD_CHARS = {

        // Define the char elements of this char array
        '_', '#', '!', '|', '?', '<', '>', '^', '(', ')', ':', ';', '/', '\\', '[', ']', '\t', '\n', '\r', '\f', '{', '}',
    };

	/** LimeWire doesn't use GUESS searching any longer. */
	public static final BooleanSetting GUESS_ENABLED = FACTORY.createBooleanSetting("GUESS_ENABLED", true);

	/** True, allow the program to get UDP packets outside of TCP socket Gnutella connections. */
	public static final BooleanSetting OOB_ENABLED = FACTORY.createBooleanSetting("OOB_ENABLED", true);

    /**
     * Not used.
     * The TTL for probe queries.
     */
    public static final ByteSetting PROBE_TTL = FACTORY.createByteSetting("PROBE_TTL", (byte)2);

    /**
     * The characters that are not allowed in seach text.
     * _ # ! | ? < > ^ ( ) : ; / \ [ ] \t \n \r \f { }
     * 
     * LimeWire's user interface blocks the user from entering these characters in the Search box.
     * The QueryRequest constructor drops packets that come in with these characters in their search text.
     */
    public static final CharArraySetting ILLEGAL_CHARS = FACTORY.createCharArraySetting("ILLEGAL_CHARS", BAD_CHARS);

    /** 30 characters, the maximum number to allow in search text. */
    public static final IntSetting MAX_QUERY_LENGTH = FACTORY.createIntSetting("MAX_QUERY_LENGTH", 30);

    /** 500 characters, the maximum number to allow in an XML query. */
    public static final IntSetting MAX_XML_QUERY_LENGTH = FACTORY.createIntSetting("MAX_XML_QUERY_LENGTH", 500);

    /**
     * 0, a search result will have to have this many stars to show up in the list.
     * The number of stars in the GUI come from the number HostData.getQuality() returns.
     */
    public static final IntSetting MINIMUM_SEARCH_QUALITY = FACTORY.createIntSetting("MINIMUM_SEARCH_QUALITY", 0);

    /**
     * 0, a search result has to have this speed to show up in the list.
     * The speed is compared to the upload speed a sharing computer reports in the payload of a query hit packet.
     */
    public static final IntSetting MINIMUM_SEARCH_SPEED = FACTORY.createIntSetting("MINIMUM_SEARCH_SPEED", 0);

    /** 5, don't let the user run more than 5 searches at once. */
    public static final IntSetting PARALLEL_SEARCH = FACTORY.createIntSetting("PARALLEL_SEARCH", 5);

    /**
     * QueryKey is a part of GUESS, and no longer used.
     * Do not issue query keys more than this often.
     */
    public static final IntSetting QUERY_KEY_DELAY = FACTORY.createSettableIntSetting("QUERY_KEY_DELAY", 500, "MessageRouter.QueryKeyDelay", 10000, 10);
}
