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
		String composite = nameStr+": "+valueStr+CRLF;
		os.write(composite.getBytes());
	}

	public static void main(String[] args) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			HTTPHeaderValue val = new AlternateLocationCollection();
			HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
								  val,
								  baos);
			System.out.println(baos); 
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
