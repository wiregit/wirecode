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
	 * Private constructor for creating the "enum" of header names.
	 *
	 * @param name the string header as it is written out to the
	 *  network
	 */
	private HTTPHeaderName(final String name) {
		NAME = name;
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
	 * Accessor to obtain the string representation of the header
	 * as it should be written out to the network.
	 */
	public String httpStringValue() {
		return NAME;
	}
}
