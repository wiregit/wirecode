package com.limegroup.gnutella.http;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.statistics.*;
import java.io.*;

/**
 * This class supplies general facilities for handling HTTP, such as
 * writing headers, extracting header values, etc..
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
	 * Cached colon to avoid excessive allocations.
	 */
	private static final String COLON = ":";

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
	 * @param value the <tt>String</tt> instance containing the
	 *  header value to write to the stream
	 * @param os the <tt>OutputStream</tt> instance to write to
	 */
	public static void writeHeader(HTTPHeaderName name, String value, 
								   OutputStream os) 
		throws IOException {
		if(name == null) {
			throw new NullPointerException("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerException("null value in writing http header: "+
										   name);
		} else if(os == null) {
			throw new NullPointerException("null os in writing http header: "+
										   name);
		}
		String header = createHeader(name, value);
		os.write(header.getBytes());
		if(!CommonUtils.isJava118()) 
			BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}

	/**
	 * Writes an single http header to the specified 
	 * <tt>OutputStream</tt> instance, with the specified header name 
	 * and the specified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instance containing the
	 *  header name to write to the stream
	 * @param value the <tt>HTTPHeaderValue</tt> instance containing the
	 *  header value to write to the stream
	 * @param out the <tt>Writer</tt> instance to write to
	 */
	public static void writeHeader(HTTPHeaderName name, String value, 
								   Writer out) 
		throws IOException {
		if(name == null) {
			throw new NullPointerException("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerException("null value in writing http header: "+
										   name);
		} else if(out == null) {
			throw new NullPointerException("null os in writing http header: "+
										   name);
		}
		String header = createHeader(name, value);
		out.write(header);
		if(!CommonUtils.isJava118()) 
			BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}
	

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
		if(name == null) {
			throw new NullPointerException("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerException("null value in writing http header: "+
										   name);
		} else if(os == null) {
			throw new NullPointerException("null os in writing http header: "+
										   name);
		}
		String header = createHeader(name, value.httpStringValue());
		os.write(header.getBytes());
		if(!CommonUtils.isJava118()) 
			BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}

	/**
	 * Writes an single http header to the specified 
	 * <tt>OutputStream</tt> instance, with the specified header name 
	 * and the specified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instance containing the
	 *  header name to write to the stream
	 * @param name the <tt>HTTPHeaderValue</tt> instance containing the
	 *  header value to write to the stream
	 * @param out the <tt>Writer</tt> instance to write to
	 */
	public static void writeHeader(HTTPHeaderName name, HTTPHeaderValue value, 
								   Writer out) 
		throws IOException {
		if(name == null) {
			throw new NullPointerException("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerException("null value in writing http header: "+
										   name);
		} else if(out == null) {
			throw new NullPointerException("null os in writing http header: "+
										   name);
		}
		String header = createHeader(name, value.httpStringValue());
		out.write(header);
		if(!CommonUtils.isJava118()) 
			BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}

	/**
	 * Create a single http header String with the specified header name 
	 * and the specified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instance containing the
	 *  header name 
	 * @param valueStr the value of the header, generally the httpStringValue
	 *  or a HttpHeaderValue, or just a String.
	 */
	private static String createHeader(HTTPHeaderName name, 
									   String valueStr) 
		throws IOException {
		if((name == null) || (valueStr == null)) {
			throw new NullPointerException("null value in creating http header");
		}
		String nameStr  = name.httpStringValue();;
		if(nameStr == null) {
			throw new NullPointerException("null value in creating http header");
		}

		StringBuffer sb = new StringBuffer();
		sb.append(nameStr);
		sb.append(COLON_SPACE);
		sb.append(valueStr);
		sb.append(CRLF);
		return sb.toString();
	}

	/**
	 * Parses out the header value from the HTTP header string.
	 *
	 * @return the header value for the specified full header string, or
	 *  <tt>null</tt> if the value could not be extracted
	 */
	public static String extractHeaderValue(final String header) {
		int index = header.indexOf(COLON);
		if(index <= 0) return null;
		return header.substring(index+1).trim();
	}
}
