package com.limegroup.gnutella;

import java.io.*;

/**
 * This class supplies general facilities for handling HTTP, such as
 * writing headers.
 */
public final class HTTPUtils {
	
	/**
	 * Constant for the carriage-return linefeed sequence that marks
	 * the end of an HTTP header
	 */
	private static final String CRLF = "\r\n";

	/**
	 * Cached colon followed by a space to avoid excessive allocations.
	 */
	private static final String COLON_SPACE = ": ";

	/**
	 * Private constructor to ensure that this class cannot be constructed
	 */
	private HTTPUtils() {}

	/**
	 * Writes an single http header to the specified 
	 * <tt>OutputStream</tt> instance, with the specified header name 
	 * and the specified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instance containing the
	 *  header name to write to the stream
	 * @param name the <tt>HTTPHeaderValue</tt> instance containing the
	 *  header value to write to the stream
	 * @param os the <tt>OutputStream</tt> instance to write to
	 */
	public static void writeHeader(HTTPHeaderName name, HTTPHeaderValue value, 
								   OutputStream os) 
		throws IOException {
		if((name == null) || (value == null) || (os == null)) {
			throw new NullPointerException("null value in writing http header");
		}
		String nameStr  = name.httpStringValue();
		String valueStr = value.httpStringValue();
		if((nameStr == null) || (valueStr == null)) {
			throw new NullPointerException("null value in writing http header");
		}		
		StringBuffer sb = new StringBuffer();
		sb.append(nameStr);
		sb.append(COLON_SPACE);
		sb.append(valueStr);
		sb.append(CRLF);
		os.write(sb.toString().getBytes());
	}

	/**
	 * Parses out the header value from the HTTP header string.
	 *
	 * @return the header value for the specified full header string, or
	 *  <tt>null</tt> if the value could not be extracted
	 */
	public static String extractHeaderValue(final String header) {
		int index = header.indexOf(":");
		if(index == 0) return null;
		return header.substring(index+2).trim();
	}
	
}
