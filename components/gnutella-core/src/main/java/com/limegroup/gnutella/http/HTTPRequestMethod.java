package com.limegroup.gnutella.http;

import java.io.*;

/**
 * Type-safe enum for HTTP request methods, as specified in RFC 2616.
 * The only required methods for HTTP 1.1 compliance are GET and HEAD.
 */
public abstract class HTTPRequestMethod {

	/**
	 * Constant for the name of the HTTP request method, in uppercase.
	 */
	private final String METHOD_NAME;

	/**
	 * Constructor creates a new <tt>HTTPRequestMethod</tt> with the 
	 * specified indentifying string.
	 *
	 * @param methodName the http request method as a string
	 */
	private HTTPRequestMethod(String methodName) {
		// store the method name in upper case to make string
		// comparisons easier
		METHOD_NAME = methodName.toUpperCase();
	}

	/**
	 * Abstract method for writing the HTTP response based on the HTTP
	 * request method.
	 *
	 * @param response the <tt>HTTPMessage</tt> instance that handles
	 *  writing the actual message
	 * @param os the <tt>OutputStream</tt> to write to
	 */
	public abstract void writeHttpResponse(HTTPMessage response, 
										   OutputStream os) 
		throws IOException;

	/**
	 * Constant for the "GET" request method.
	 */
	public static final HTTPRequestMethod GET = 
		new HTTPRequestMethod("GET") {
			public void writeHttpResponse(HTTPMessage response, OutputStream os) 
			    throws IOException {
				response.writeMessageHeaders(os);
				response.writeMessageBody(os);
				os.flush();
			}
		};

	/**
	 * Constant for the "HEAD" request method.
	 */
	public static final HTTPRequestMethod HEAD = 
		new HTTPRequestMethod("HEAD") {
			public void writeHttpResponse(HTTPMessage response, OutputStream os) 
			    throws IOException {
				response.writeMessageHeaders(os);
				os.flush();
			}
		};
}
