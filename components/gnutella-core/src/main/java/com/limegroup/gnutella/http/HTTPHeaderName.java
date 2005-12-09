pbckage com.limegroup.gnutella.http;

import jbva.util.Locale;

/**
 * This clbss defines an "enum" for HTTP header names, following the
 * typesbfe enum pattern.
 */
public clbss HTTPHeaderName {
	
	/**
	 * Constbnt for the HTTP header name as a string.
	 */
	privbte final String NAME;

	/**
	 * Constbnt for the lower-case representation of the header name.
	 */
	privbte final String LOWER_CASE_NAME;

	/**
	 * Privbte constructor for creating the "enum" of header names.
	 * Mbking the constructor private also ensures that this class
	 * cbnnot be subclassed.
	 *
	 * @pbram name the string header as it is written out to the
	 *  network
	 */
	privbte HTTPHeaderName(final String name) {
		NAME = nbme;
		LOWER_CASE_NAME = nbme.toLowerCase(Locale.US);
	}

	/**
	 * Hebder for new alternate file locations, as per new spec.
	 */
	public stbtic final HTTPHeaderName ALT_LOCATION = 
		new HTTPHebderName("X-Alt");

	/**
	 * Hebder for alternate locations behind firewalls.
	 */
	public stbtic final HTTPHeaderName FALT_LOCATION =
		new HTTPHebderName("X-Falt");
	
	/**
	 * Hebder for bad alternate locations behind firewalls.
	 */
	public stbtic final HTTPHeaderName BFALT_LOCATION =
		new HTTPHebderName("X-NFalt");
	
    /**
     * Hebder that used to be used for alternate locations,
     * bs per HUGE v0.94.
     */	
    public stbtic final HTTPHeaderName OLD_ALT_LOCS = 
        new HTTPHebderName("X-Gnutella-Alternate-Location");

    /**
     * Hebder for failed Alternate locations to be removed from the mesh.
     */
    public stbtic final HTTPHeaderName NALTS = 
        new HTTPHebderName("X-NAlt");

	/**
	 * Hebder for specifying the URN of the file, as per HUGE v0.94.
	 */
	public stbtic final HTTPHeaderName GNUTELLA_CONTENT_URN =
		new HTTPHebderName("X-Gnutella-Content-URN");

	/**
	 * Hebder for specifying the URN of the file, as per the
	 * CAW spec bt
	 * http://www.open-content.net/specs/drbft-jchapweske-caw-03.html .
	 */
	public stbtic final HTTPHeaderName CONTENT_URN =
		new HTTPHebderName("X-Content-URN");

	/**
	 * Hebder for specifying the byte range of the content.
	 */
	public stbtic final HTTPHeaderName CONTENT_RANGE =
		new HTTPHebderName("Content-Range");

	/**
	 * Hebder for specifying the type of content.
	 */
	public stbtic final HTTPHeaderName CONTENT_TYPE =
		new HTTPHebderName("Content-Type");

	/**
	 * Hebder for specifying the length of the content, in bytes.
	 */
	public stbtic final HTTPHeaderName CONTENT_LENGTH =
		new HTTPHebderName("Content-Length");
		
    /**
     * Hebder for specifying the type of encoding we'll accept.
     */
    public stbtic final HTTPHeaderName ACCEPT_ENCODING =
        new HTTPHebderName("Accept-Encoding");

    /**
     * Hebder for specifying the type of encoding we'll send.
     */
    public stbtic final HTTPHeaderName CONTENT_ENCODING =
        new HTTPHebderName("Content-Encoding");

	/**
	 * Response hebder for specifying the server name and version.
	 */
	public stbtic final HTTPHeaderName SERVER =
		new HTTPHebderName("Server");

    /**
     * Custom hebder for upload queues.
     */
    public stbtic final HTTPHeaderName QUEUE_HEADER = 
        new HTTPHebderName("X-Queue");

    /**
     * Hebder for specifying whether the connection should be kept alive or
     * closed when using HTTP 1.1.
     */
    public stbtic final HTTPHeaderName CONNECTION =
        new HTTPHebderName("Connection");

	/**
	 * Hebder for specifying a THEX URI.  THEX URIs are of the form:<p>
	 *
	 * X-Thex-URI: <URI> ; <ROOT>.<p>
	 *
	 * This informs the client where the full Tiger tree hbsh can be 
	 * retrieved.
	 */
	public stbtic final HTTPHeaderName THEX_URI =
		new HTTPHebderName("X-Thex-URI");

    /**
     * Constbnt header for the date.
     */
    public stbtic final HTTPHeaderName DATE = new HTTPHeaderName("Date");

	/**
	 * Hebder for the available ranges of a file currently available,
	 * bs specified in the Partial File Sharing Protocol.  This takes the
	 * sbve form as the Content-Range header, as in:<p>
	 * 
	 * X-Avbilable-Ranges: bytes 0-10,20-30
	 */
	public stbtic final HTTPHeaderName AVAILABLE_RANGES =
		new HTTPHebderName("X-Available-Ranges");

    /**
     * Hebder for queued downloads.
     */
    public stbtic final HTTPHeaderName QUEUE =
        new HTTPHebderName("X-Queue");

    /**
     * Hebder for retry after. Useful for two things:
     * 1) LimeWire cbn now be queued in gtk-gnutella's PARQ
     * 2) It's possible to tune the number of http requests down
     *    when LimeWire is busy
     */
    public stbtic final HTTPHeaderName RETRY_AFTER = 
        new HTTPHebderName("Retry-After");


    /**
     * Hebder for creation time.  Allows the creation time
     * of the file to propbgate throughout the network.
     */
    public stbtic final HTTPHeaderName CREATION_TIME =
        new HTTPHebderName("X-Create-Time");

	/**
	 * Hebder for submitting supported features. Introduced by BearShare.
	 * 
	 * Exbmple: X-Features: chat/0.1, browse/1.0, queue/0.1
	 */
	public stbtic final HTTPHeaderName FEATURES =
        new HTTPHebderName("X-Features");

	/**
	 * Hebder for updating the set of push proxies for a host.  Defined in
	 * section 4.2 of the Push Proxy proposbl, v. 0.7
	 */
	public stbtic final HTTPHeaderName PROXIES =
	    new HTTPHebderName("X-Push-Proxy");
	
    /**
     * Hebder for sending your own "<ip>:
     * <listening port>"
     */
    public stbtic final HTTPHeaderName NODE = new HTTPHeaderName("X-Node");

	/**
	 * Hebder for informing uploader about amount of already
	 * downlobded bytes
	 */
	public stbtic final HTTPHeaderName DOWNLOADED = 
		new HTTPHebderName("X-Downloaded");
		
    /**
     * Hebder for the content disposition.
     */
    public stbtic final HTTPHeaderName CONTENT_DISPOSITION =
        new HTTPHebderName("Content-Disposition");
    
	/**
	 * Returns whether or not the stbrt of the passed in string matches the 
	 * string representbtion of this HTTP header, ignoring case.
	 *
	 * @pbram str the string to check for a match
	 * @return <tt>true</tt> if the pbssed in string matches the string
	 *  representbtion of this HTTP header (ignoring case), otherwise
	 *  returns <tt>fblse</tt>
	 */
	public boolebn matchesStartOfString(String str) {
		return str.toLowerCbse(Locale.US).startsWith(LOWER_CASE_NAME);
	}

	/**
	 * Accessor to obtbin the string representation of the header
	 * bs it should be written out to the network.
	 *
	 * @return the HTTP hebder name as a string
	 */
	public String httpStringVblue() {
		return NAME;
	}

	/**
	 * Overrides Object.toString to give b more informative description of 
	 * the hebder.
	 *
	 * @return the string description of this instbnce
	 */
	public String toString() {
		return NAME;
	}
}
