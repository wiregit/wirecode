package com.limegroup.gnutella.http;

import java.util.Locale;

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
		LOWER_CASE_NAME = name.toLowerCase(Locale.US);
	}

	/**
	 * Header for alternate file locations, as per HUGE v0.94.
	 */
	public static final HTTPHeaderName ALT_LOCATION=new HTTPHeaderName("X-Alt");
	
    public static final HTTPHeaderName OLD_ALT_LOCS = 
        new HTTPHeaderName("X-Gnutella-Alternate-Location");
    

    /**
     * Header for failed Alternate locations to be added to mesh
     */
    public static final HTTPHeaderName NALTS = new HTTPHeaderName("X-NAlt");
    

	/**
	 * Header for specifying the URN of the file, as per HUGE v0.94.
	 */
	public static final HTTPHeaderName GNUTELLA_CONTENT_URN =
		new HTTPHeaderName("X-Gnutella-Content-URN");

	/**
	 * Header for specifying the URN of the file, as per HUGE v0.94.
	 */
	//public static final HTTPHeaderName CONTENT_URN =
	//new HTTPHeaderName("X--Content-URN");

	/**
	 * Header for specifying the byte range of the content.
	 */
	public static final HTTPHeaderName CONTENT_RANGE =
		new HTTPHeaderName("Content-Range");

	/**
	 * Header for specifying the type of content.
	 */
	public static final HTTPHeaderName CONTENT_TYPE =
		new HTTPHeaderName("Content-Type");

	/**
	 * Header for specifying the length of the content, in bytes.
	 */
	public static final HTTPHeaderName CONTENT_LENGTH =
		new HTTPHeaderName("Content-Length");
		
    /**
     * Header for specifying the type of encoding we'll accept.
     */
    public static final HTTPHeaderName ACCEPT_ENCODING =
        new HTTPHeaderName("Accept-Encoding");

    /**
     * Header for specifying the type of encoding we'll send.
     */
    public static final HTTPHeaderName CONTENT_ENCODING =
        new HTTPHeaderName("Content-Encoding");

	/**
	 * Response header for specifying the server name and version.
	 */
	public static final HTTPHeaderName SERVER =
		new HTTPHeaderName("Server");

    /**
     * Custom header for upload queues.
     */
    public static final HTTPHeaderName QUEUE_HEADER = 
        new HTTPHeaderName("X-Queue");

    /**
     * Header for specifying whether the connection should be kept alive or
     * closed when using HTTP 1.1.
     */
    public static final HTTPHeaderName CONNECTION =
        new HTTPHeaderName("Connection");

	/**
	 * Header for specifying a THEX URI.  THEX URIs are of the form:<p>
	 *
	 * X-Thex-URI: <URI> ; <ROOT>.<p>
	 *
	 * This informs the client where the full Tiger tree hash can be 
	 * retrieved.
	 */
	public static final HTTPHeaderName THEX_URI =
		new HTTPHeaderName("X-Thex-URI");

	/**
	 * Header for the available ranges of a file currently available,
	 * as specified in the Partial File Sharing Protocol.  This takes the
	 * save form as the Content-Range header, as in:<p>
	 * 
	 * X-Available-Ranges: bytes 0-10,20-30
	 */
	public static final HTTPHeaderName AVAILABLE_RANGES =
		new HTTPHeaderName("X-Available-Ranges");

    /**
     * Header for queued downloads.
     */
    public static final HTTPHeaderName QUEUE =
        new HTTPHeaderName("X-Queue");
    

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
		return str.toLowerCase(Locale.US).startsWith(LOWER_CASE_NAME);
	}

	/**
	 * Accessor to obtain the string representation of the header
	 * as it should be written out to the network.
	 *
	 * @return the HTTP header name as a string
	 */
	public String httpStringValue() {
		return NAME;
	}

	/**
	 * Overrides Object.toString to give a more informative description of 
	 * the header.
	 *
	 * @return the string description of this instance
	 */
	public String toString() {
		return NAME;
	}
}
