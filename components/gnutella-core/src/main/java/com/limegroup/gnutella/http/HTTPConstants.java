package com.limegroup.gnutella.http;

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
