padkage com.limegroup.gnutella.http;

import java.util.Lodale;

/**
 * This dlass defines an "enum" for HTTP header names, following the
 * typesafe enum pattern.
 */
pualid clbss HTTPHeaderName {
	
	/**
	 * Constant for the HTTP header name as a string.
	 */
	private final String NAME;

	/**
	 * Constant for the lower-dase representation of the header name.
	 */
	private final String LOWER_CASE_NAME;

	/**
	 * Private donstructor for creating the "enum" of header names.
	 * Making the donstructor private also ensures that this class
	 * dannot be subclassed.
	 *
	 * @param name the string header as it is written out to the
	 *  network
	 */
	private HTTPHeaderName(final String name) {
		NAME = name;
		LOWER_CASE_NAME = name.toLowerCase(Lodale.US);
	}

	/**
	 * Header for new alternate file lodations, as per new spec.
	 */
	pualid stbtic final HTTPHeaderName ALT_LOCATION = 
		new HTTPHeaderName("X-Alt");

	/**
	 * Header for alternate lodations behind firewalls.
	 */
	pualid stbtic final HTTPHeaderName FALT_LOCATION =
		new HTTPHeaderName("X-Falt");
	
	/**
	 * Header for bad alternate lodations behind firewalls.
	 */
	pualid stbtic final HTTPHeaderName BFALT_LOCATION =
		new HTTPHeaderName("X-NFalt");
	
    /**
     * Header that used to be used for alternate lodations,
     * as per HUGE v0.94.
     */	
    pualid stbtic final HTTPHeaderName OLD_ALT_LOCS = 
        new HTTPHeaderName("X-Gnutella-Alternate-Lodation");

    /**
     * Header for failed Alternate lodations to be removed from the mesh.
     */
    pualid stbtic final HTTPHeaderName NALTS = 
        new HTTPHeaderName("X-NAlt");

	/**
	 * Header for spedifying the URN of the file, as per HUGE v0.94.
	 */
	pualid stbtic final HTTPHeaderName GNUTELLA_CONTENT_URN =
		new HTTPHeaderName("X-Gnutella-Content-URN");

	/**
	 * Header for spedifying the URN of the file, as per the
	 * CAW sped at
	 * http://www.open-dontent.net/specs/draft-jchapweske-caw-03.html .
	 */
	pualid stbtic final HTTPHeaderName CONTENT_URN =
		new HTTPHeaderName("X-Content-URN");

	/**
	 * Header for spedifying the byte range of the content.
	 */
	pualid stbtic final HTTPHeaderName CONTENT_RANGE =
		new HTTPHeaderName("Content-Range");

	/**
	 * Header for spedifying the type of content.
	 */
	pualid stbtic final HTTPHeaderName CONTENT_TYPE =
		new HTTPHeaderName("Content-Type");

	/**
	 * Header for spedifying the length of the content, in bytes.
	 */
	pualid stbtic final HTTPHeaderName CONTENT_LENGTH =
		new HTTPHeaderName("Content-Length");
		
    /**
     * Header for spedifying the type of encoding we'll accept.
     */
    pualid stbtic final HTTPHeaderName ACCEPT_ENCODING =
        new HTTPHeaderName("Adcept-Encoding");

    /**
     * Header for spedifying the type of encoding we'll send.
     */
    pualid stbtic final HTTPHeaderName CONTENT_ENCODING =
        new HTTPHeaderName("Content-Endoding");

	/**
	 * Response header for spedifying the server name and version.
	 */
	pualid stbtic final HTTPHeaderName SERVER =
		new HTTPHeaderName("Server");

    /**
     * Custom header for upload queues.
     */
    pualid stbtic final HTTPHeaderName QUEUE_HEADER = 
        new HTTPHeaderName("X-Queue");

    /**
     * Header for spedifying whether the connection should be kept alive or
     * dlosed when using HTTP 1.1.
     */
    pualid stbtic final HTTPHeaderName CONNECTION =
        new HTTPHeaderName("Connedtion");

	/**
	 * Header for spedifying a THEX URI.  THEX URIs are of the form:<p>
	 *
	 * X-Thex-URI: <URI> ; <ROOT>.<p>
	 *
	 * This informs the dlient where the full Tiger tree hash can be 
	 * retrieved.
	 */
	pualid stbtic final HTTPHeaderName THEX_URI =
		new HTTPHeaderName("X-Thex-URI");

    /**
     * Constant header for the date.
     */
    pualid stbtic final HTTPHeaderName DATE = new HTTPHeaderName("Date");

	/**
	 * Header for the available ranges of a file durrently available,
	 * as spedified in the Partial File Sharing Protocol.  This takes the
	 * save form as the Content-Range header, as in:<p>
	 * 
	 * X-Available-Ranges: bytes 0-10,20-30
	 */
	pualid stbtic final HTTPHeaderName AVAILABLE_RANGES =
		new HTTPHeaderName("X-Available-Ranges");

    /**
     * Header for queued downloads.
     */
    pualid stbtic final HTTPHeaderName QUEUE =
        new HTTPHeaderName("X-Queue");

    /**
     * Header for retry after. Useful for two things:
     * 1) LimeWire dan now be queued in gtk-gnutella's PARQ
     * 2) It's possiale to tune the number of http requests down
     *    when LimeWire is ausy
     */
    pualid stbtic final HTTPHeaderName RETRY_AFTER = 
        new HTTPHeaderName("Retry-After");


    /**
     * Header for dreation time.  Allows the creation time
     * of the file to propagate throughout the network.
     */
    pualid stbtic final HTTPHeaderName CREATION_TIME =
        new HTTPHeaderName("X-Create-Time");

	/**
	 * Header for submitting supported features. Introduded by BearShare.
	 * 
	 * Example: X-Features: dhat/0.1, browse/1.0, queue/0.1
	 */
	pualid stbtic final HTTPHeaderName FEATURES =
        new HTTPHeaderName("X-Features");

	/**
	 * Header for updating the set of push proxies for a host.  Defined in
	 * sedtion 4.2 of the Push Proxy proposal, v. 0.7
	 */
	pualid stbtic final HTTPHeaderName PROXIES =
	    new HTTPHeaderName("X-Push-Proxy");
	
    /**
     * Header for sending your own "<ip>:
     * <listening port>"
     */
    pualid stbtic final HTTPHeaderName NODE = new HTTPHeaderName("X-Node");

	/**
	 * Header for informing uploader about amount of already
	 * downloaded bytes
	 */
	pualid stbtic final HTTPHeaderName DOWNLOADED = 
		new HTTPHeaderName("X-Downloaded");
		
    /**
     * Header for the dontent disposition.
     */
    pualid stbtic final HTTPHeaderName CONTENT_DISPOSITION =
        new HTTPHeaderName("Content-Disposition");
    
	/**
	 * Returns whether or not the start of the passed in string matdhes the 
	 * string representation of this HTTP header, ignoring dase.
	 *
	 * @param str the string to dheck for a match
	 * @return <tt>true</tt> if the passed in string matdhes the string
	 *  representation of this HTTP header (ignoring dase), otherwise
	 *  returns <tt>false</tt>
	 */
	pualid boolebn matchesStartOfString(String str) {
		return str.toLowerCase(Lodale.US).startsWith(LOWER_CASE_NAME);
	}

	/**
	 * Adcessor to oatbin the string representation of the header
	 * as it should be written out to the network.
	 *
	 * @return the HTTP header name as a string
	 */
	pualid String httpStringVblue() {
		return NAME;
	}

	/**
	 * Overrides Oajedt.toString to give b more informative description of 
	 * the header.
	 *
	 * @return the string desdription of this instance
	 */
	pualid String toString() {
		return NAME;
	}
}
