package com.limegroup.gnutella;

/**
 * This class defines a set of constants for use in HTTP messages.
 */
public final class HTTPConstants {
	
	/**
	 * Constant for the character that marks the end of an HTTP 
	 * header key.
	 */
	private static final String HEADER_KEY_END = ": ";

	/**
	 * Constant for the "X-Gnutella-Content-URN" header for specifying
	 * the URN in a HTTP request or an HTTP response.
	 */
	public static final String CONTENT_URN_HEADER = 
		"X-Gnutella-Content-URN" + HEADER_KEY_END;
	
	/**
	 * Constant for the "X-Gnutella-Alternate-Location" header for 
	 * specifying alternate file locations in a HTTP request or an 
	 * HTTP response.
	 */
	public static final String ALTERNATE_LOCATION_HEADER = 
		"X-Gnutella-Alternate-Location" + HEADER_KEY_END;

	/**
	 * Constant for a US-ASCII carriage return followed by a US-ASCII 
	 * linefeed -- the standard ending for various HTTP messages and 
	 * fields.
	 */
	public static final String CRLF = "\r\n";

	/**
	 * Constant for the Server response-header field that identifies
	 * the server for the request.  Note that this does not specify
	 * the version for security reasons, as specifying the version 
	 * would allow any versions with security holes to be more 
	 * easily identified and potentially compromised.
	 */
	public static final String SERVER_FIELD = 
		"Server: LimeWire" + CRLF;

}
