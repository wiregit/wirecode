package com.limegroup.gnutella;

/**
 * This class defines an "enum" for HTTP header names, following the
 * typesafe enum pattern.
 */
public class HTTPHeaderName {
	
	/**
	 * Constant for the HTTP header name as a string.
	 */
	private final String NAME;

	/**
	 * Constant for the lower-case representation of the header name.
	 */
	private final String LOWER_CASE_NAME;

	/**
	 * Private constructor for creating the "enum" of header names.
	 * Making the constructor private also ensures that this class
	 * cannot be subclassed.
	 *
	 * @param name the string header as it is written out to the
	 *  network
	 */
	private HTTPHeaderName(final String name) {
		NAME = name;
		LOWER_CASE_NAME = name.toLowerCase();
	}

	/**
	 * Header for alternate file locations, as per HUGE v0.94.
	 */
	public static final HTTPHeaderName ALT_LOCATION = 
		new HTTPHeaderName("X-Gnutella-Alternate-Location");
	
	/**
	 * Header for specifying the URN of the file, as per HUGE v0.94.
	 */
	public static final HTTPHeaderName CONTENT_URN =
		new HTTPHeaderName("X-Gnutella-Content-URN");

	/**
	 * Returns whether or not the start of the passed in string matches the 
	 * string representation of this HTTP header, ignoring case.
	 *
	 * @param str the string to check for a match
	 * @return <tt>true</tt> if the passed in string matches the string
	 *  representation of this HTTP header (ignoring case), otherwise
	 *  returns <tt>false</tt>
	 */
	public boolean matchesStartOfString(String str) {
		return LOWER_CASE_NAME.startsWith(str.toLowerCase());
	}

	/**
	 * Accessor to obtain the string representation of the header
	 * as it should be written out to the network.
	 */
	public String httpStringValue() {
		return NAME;
	}
}
