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
	 * Header for new alternate file locations, as per new spec.
	 */
	public static final HTTPHeaderName ALT_LOCATION = 
		new HTTPHeaderName("X-Alt");

	/**
	 * Header for alternate locations behind firewalls.
	 */
	public static final HTTPHeaderName FALT_LOCATION =
		new HTTPHeaderName("X-Falt");
	
	/**
	 * Header for bad alternate locations behind firewalls.
	 */
	public static final HTTPHeaderName BFALT_LOCATION =
		new HTTPHeaderName("X-NFalt");
	
    /**
     * Header that used to be used for alternate locations,
     * as per HUGE v0.94.
     */	
    public static final HTTPHeaderName OLD_ALT_LOCS = 
        new HTTPHeaderName("X-Gnutella-Alternate-Location");

    /**
     * Header for failed Alternate locations to be removed from the mesh.
     */
    public static final HTTPHeaderName NALTS = 
        new HTTPHeaderName("X-NAlt");

	/**
	 * Header for specifying the URN of the file, as per HUGE v0.94.
	 */
	public static final HTTPHeaderName GNUTELLA_CONTENT_URN =
		new HTTPHeaderName("X-Gnutella-Content-URN");

	/**
	 * Header for specifying the URN of the file, as per the
	 * CAW spec at
	 * http://www.open-content.net/specs/draft-jchapweske-caw-03.html .
	 */
	public static final HTTPHeaderName CONTENT_URN =
		new HTTPHeaderName("X-Content-URN");

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
     * Constant header for the date.
     */
    public static final HTTPHeaderName DATE = new HTTPHeaderName("Date");

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
     * Header for retry after. Useful for two things:
     * 1) LimeWire can now be queued in gtk-gnutella's PARQ
     * 2) It's possible to tune the number of http requests down
     *    when LimeWire is busy
     */
    public static final HTTPHeaderName RETRY_AFTER = 
        new HTTPHeaderName("Retry-After");


    /**
     * Header for creation time.  Allows the creation time
     * of the file to propagate throughout the network.
     */
    public static final HTTPHeaderName CREATION_TIME =
        new HTTPHeaderName("X-Create-Time");

	/**
	 * Header for submitting supported features. Introduced by BearShare.
	 * 
	 * Example: X-Features: chat/0.1, browse/1.0, queue/0.1
	 */
	public static final HTTPHeaderName FEATURES =
        new HTTPHeaderName("X-Features");

	/**
	 * Header for updating the set of push proxies for a host.  Defined in
	 * section 4.2 of the Push Proxy proposal, v. 0.7
	 */
	public static final HTTPHeaderName PROXIES =
	    new HTTPHeaderName("X-Push-Proxy");
	
    /**
     * Header for sending your own "<ip>:
     * <listening port>"
     */
    public static final HTTPHeaderName NODE = new HTTPHeaderName("X-Node");

	/**
	 * Header for informing uploader about amount of already
	 * downloaded bytes
	 */
	public static final HTTPHeaderName DOWNLOADED = 
		new HTTPHeaderName("X-Downloaded");
		
    /**
     * Header for the content disposition.
     */
    public static final HTTPHeaderName CONTENT_DISPOSITION =
        new HTTPHeaderName("Content-Disposition");
    
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
