package com.limegroup.gnutella;

/**
 * This class defines a set of constants for use in HTTP messages.
 */
public final class HTTPConstants {

    /**
     * Private constructor to ensure that this class cannot be
     * instantiated.
     */
    private HTTPConstants() {}
	
	/**
	 * Constant for the character that marks the end of an HTTP 
	 * header key.
	 */
	private static final String HEADER_KEY_END = ":";

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

	/**
	 * Constant for the beginning "GET" of an HTTP URN get request.
	 */
	public static final String GET = "GET";

	/**
	 * Constant for the HTTP 1.0 specifier at the end of an HTTP URN get
	 * request.
	 */
	public static final String HTTP10 = "HTTP/1.0";
	
	/**
	 * Constant for the HTTP 1.1 specifier at the end of an HTTP URN get
	 * request.
	 */
	public static final String HTTP11 = "HTTP/1.1";

	/**
	 * Constant for the "uri-res" specifier for an HTTP URN get request.
	 */
	public static final String URI_RES = "uri-res";

	/**
	 * Constant for the "Name to Resource", or "N2R?" resolution 
	 * service identifier, as specified in RFC 2169.
	 */
	public static final String NAME_TO_RESOURCE = "N2R?"; 	

	/**
	 * Constant for the "uri-res" uri resolution specifier, followed by
	 * the standard "/" and the resolution service id, in our case "N2R?".
	 */
	public static final String URI_RES_N2R = "/"+URI_RES+"/"+NAME_TO_RESOURCE;

}
