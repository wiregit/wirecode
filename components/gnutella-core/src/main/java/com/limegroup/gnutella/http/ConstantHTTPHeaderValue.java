package com.limegroup.gnutella.http;

import com.limegroup.gnutella.util.*;

/**
 * This class adds type safety for constant HTTP header values.  If there's
 * an HTTP header value that is constant, simply add it to this enumeration.
 */
public class ConstantHTTPHeaderValue implements HTTPHeaderValue {
	
	/**
	 * Constant for the value for the HTTP header.
	 */
	private final String VALUE;

	/**
	 * Creates a new <tt>ConstantHTTPHeaderValue</tt> with the specified
	 * string value.
	 *
	 * @param value the string value to return as the value for an
	 *  HTTP header
	 */
	private ConstantHTTPHeaderValue(String value) {
		VALUE = value;
	}
	
	// implements HTTPHeaderValue -- inherit doc comment
	public String httpStringValue() {
		return VALUE;
	}

	/**
	 * Constant for the HTTP server, as given in the "Server: " header.
	 */
	public static final HTTPHeaderValue SERVER_VALUE = 
		new ConstantHTTPHeaderValue(CommonUtils.getHttpServer());
	
}
