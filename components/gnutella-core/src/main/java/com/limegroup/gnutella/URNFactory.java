package com.limegroup.gnutella;

import com.limegroup.gnutella.http.*; 
import java.io.*;

/**
 * This class is a factory for creating new <tt>URN</tt> instances.  It can
 * create <tt>URN</tt>s containing SHA1 hashes, for example.  It can also
 * do so from an HTTP request line that includes a URN as long as the
 * request is in the format specified in RFC 2169.
 */
public final class URNFactory {

	/**
	 * Ensure that this class can never be constructed.
	 */
	private URNFactory() {}

	/**
	 * Cached constant to avoid making unnecessary string allocations
	 * in validating input.
	 */
	private static final String SPACE = " ";

	/**
	 * Cached constant to avoid making unnecessary string allocations
	 * in validating input.
	 */
	private static final String QUESTION_MARK = "?";

	/**
	 * Cached constant to avoid making unnecessary string allocations
	 * in validating input.
	 */
	private static final String SLASH = "/";

	/**
	 * Cached constant to avoid making unnecessary string allocations
	 * in validating input.
	 */
	private static final String TWO = "2";

	/**
	 * Creates a new <tt>URN</tt> instance with a SHA1 hash.
	 *
	 * @param file the <tt>File</tt> instance to use to create a 
	 *  <tt>URN</tt>
	 * @return a new <tt>URN</tt> instance
	 * @throws <tt>IOException</tt> if there was an error constructing
	 *  the <tt>URN</tt>
	 */
	public static URN createSHA1Urn(File file) 
		throws IOException {
		return new URN(file, UrnType.SHA1);//URN.SHA1_URN);
	}

	/**
	 * Creates a new <tt>URN</tt> instance from the specified string.
	 * The resulting URN can had any Namespace Identifier and any
	 * Namespace Specific String.
	 *
	 * @param urnString the string instance to use to create a 
	 *  <tt>URN</tt>
	 * @return a new <tt>URN</tt> instance
	 * @throws <tt>IOException</tt> if there was an error constructing
	 *  the <tt>URN</tt>
	 */
	public static URN createSHA1Urn(final String urnString) 
		throws IOException {
		return new URN(urnString);
	}

	/**
	 * Creates a URN instance from the specified HTTP request line.
	 * The request must be in the standard from, as specified in
	 * RFC 2169.  Note that com.limegroup.gnutella.Acceptor parses out
	 * the first word in the request, such as "GET" or "HEAD."
	 *
	 * @param requestLine the URN HTTP GET request of the form specified in
	 *  RFC 2169, for example:<p>
	 * 
	 * 	/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1
	 * @return a new <tt>URN</tt> instance from the specified request, or 
	 *  <tt>null</tt> if no <tt>URN</tt> could be created
	 *
	 * @see com.limegroup.gnutella.Acceptor
	 */
	public static URN createSHA1UrnFromHttpRequest(final String requestLine) 
		throws IOException {
		if(!URNFactory.isValidUrnGetRequest(requestLine)) {
			throw new IOException("INVALID URN GET REQUEST");
		}
		String urnStr = URNFactory.extractUrnFromHttpRequest(requestLine);
		if(urnStr == null) {
			throw new IOException("COULD NOT CONSTRUCT URN");
		}	   

		// this constructor can also throw an IOException
		return new URN(urnStr);
	}

	/**
	 * Creates a new <tt>URN</tt> instance from the given "service
	 * request" line.  The service request syntax must be of the form 
	 * specified in RFC 2169.  Note the the service request in this 
	 * case does not include the leading "GET" or the trailing 
	 * "HTTP/1.x".  Rather, the string must be of the form:<p>
	 * 
	 * "/uri-res/<service>?<uri>"  <p>
	 *
	 * In our case, this will mean, for example:<p>
	 *
	 * "/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"
	 */
	public static URN createSHA1UrnFromServiceRequest(final String line) 
		throws IOException {
		if(!URNFactory.isValidUriRes(line)) {
			throw new IOException("INVALID URI-RES LINE");
		}

		if(!URNFactory.isValidResolutionProtocol(line)) {
			throw new IOException("INVALID RESOLUTION PROTOCOL IN "+
								  "URI-RES LINE");
		}
		String urnStr = URNFactory.extractUrnFromServiceRequest(line);
		if(urnStr == null) {
			throw new IOException("COULD NOT CONSTRUCT A URN FROM "+
								  "THE GIVEN SERVICE REQUEST LINE");
		}
		return new URN(urnStr);		
	}

	/**
	 * Creates a new <tt>URN</tt> instance from the X-Gnutella-Content-URN
	 * HTTP header of the form specified in the HUGE v0.94 specification.
	 *
	 * @param header the full HTTP header line
	 * @return a new <tt>URN</tt> instance for the URN string contained in
	 *  the header
	 * @throws <tt>IOException</tt> if the HTTP header string is not in a 
	 *  valid form, such as when it does not contain the 
	 *  X-Gnutella-Content-URN header or when it does not contain an URN
	 *  of the expected form
	 */
	public static URN createUrnFromContentUrnHttpHeader(final String header) 
		throws IOException {
		int index = header.indexOf(HTTPHeaderName.CONTENT_URN.httpStringValue());
		if(index == -1) {
			throw new IOException("INVALID FORMAT FOR CONTENT URN HEADER: "+
								  header);
		}
		return new URN(header.substring(
		    HTTPHeaderName.CONTENT_URN.httpStringValue().length()+1).trim());
	}

	/**
	 * Returns a new <tt>String</tt> for an HTTP request for the
	 * specified URN. This follows the syntax of RFC 2169, although
	 * it does not include the leading "GET" and the trailing
	 * "HTTP/1.x".  Rather, this will return a string for the file
	 * to request, of the form:<p>
	 * 
	 * "/uri-res/<service>?<uri>"  <p>
	 *
	 * In our case, this will mean, for example:<p>
	 *
	 * "/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"
	 */
	public static String createHttpUrnServiceRequest(final URN urn) {
		return HTTPConstants.URI_RES_N2R + urn.httpStringValue();
	}

	/**
	 * Returns a <tt>String</tt> containing the URN for the get request.  For
	 * a typical SHA1 request, this will return a 41 character URN, including
	 * the 32 character hash value.
	 *
	 * @param requestLine the <tt>String</tt> instance containing the get request
	 * @return a <tt>String</tt> containing the URN for the get request, or 
	 *  <tt>null</tt> if the request could not be read
	 */
	private static String extractUrnFromHttpRequest(final String requestLine) {
		int qIndex     = requestLine.indexOf(QUESTION_MARK) + 1;
		int spaceIndex = requestLine.indexOf(SPACE, qIndex);		
		if((qIndex == -1) || (spaceIndex == -1)) {
			return null;
		}
		return requestLine.substring(qIndex, spaceIndex);
	}

	/**
	 * Returns the URN string for the specified URN service request.  The
	 * service request must be of the form:<p>
	 *
	 * "/uri-res/<service>?<uri>"  <p>
	 *
	 * In our case, this will mean, for example:<p>
	 *
	 * "/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"
	 */
	private static String extractUrnFromServiceRequest(final String urnLine) {
		int questionIndex = urnLine.indexOf(QUESTION_MARK);
		if(questionIndex != 12) return null;
		return urnLine.substring(questionIndex+1);
	}

	/**
	 * Returns whether or not the get request is valid, as specified in
	 * HUGE v. 0.93 and IETF RFC 2169.  This verifies everything except
	 * whether or not the URN itself is valid -- the URN constructor
	 * can do that, however.
	 *
	 * @param requestLine the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the reques is valid, <tt>false</tt> otherwise
	 */
	private static boolean isValidUrnGetRequest(final String requestLine) {
		return (URNFactory.isValidSize(requestLine) &&
				URNFactory.isValidUriRes(requestLine) &&
				URNFactory.isValidResolutionProtocol(requestLine) && 
				URNFactory.isValidHTTPSpecifier(requestLine));				
	}

	/** 
	 * Returns whether or not the specified get request meets size 
	 * requirements.
	 *
	 * @param requestLine the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the get request starts with "GET "
	 *  (case-insensitive), <tt>false</tt> otherwise
	 */
	private static final boolean isValidSize(final String requestLine) {
		int size = requestLine.length();
		if((size != 63) && (size != 107)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the get request corresponds with the standard 
	 * uri-res request
	 *
	 * @param requestLine the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the get request includes the standard "uri-res"
	 *  (case-insensitive) request, <tt>false</tt> otherwise
	 */
	private static final boolean isValidUriRes(final String requestLine) {
		int firstSlash = requestLine.indexOf(SLASH);
		if(firstSlash == -1) {
			return false;
		}
		int secondSlash = requestLine.indexOf(SLASH, firstSlash+1);
		if(secondSlash == -1) {
			return false;
		}
		String uriStr = requestLine.substring(firstSlash+1, secondSlash);
		if(!uriStr.equalsIgnoreCase(HTTPConstants.URI_RES)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the "resolution protocol" for the given URN GET
	 * line is valid.  We currently only support N2R, which specifies "Given 
	 * a URN, return the named resource."
	 *
	 * @param requestLine the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the resolution protocol is valid, <tt>false</tt>
	 *  otherwise
	 */
	private static boolean isValidResolutionProtocol(final String requestLine) {
		int nIndex = requestLine.indexOf(TWO);
		if(nIndex == -1) {
			return false;
		}
		String n2r = requestLine.substring(nIndex-1, nIndex+3);

		// we could add more protocols to this check
		if(!n2r.equalsIgnoreCase(HTTPConstants.NAME_TO_RESOURCE)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the HTTP specifier for the URN get request
	 * is valid.
	 *
	 * @param requestLine the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the HTTP specifier is valid, <tt>false</tt>
	 *  otherwise
	 */
	private static boolean isValidHTTPSpecifier(final String requestLine) {
		int spaceIndex = requestLine.lastIndexOf(SPACE);
		if(spaceIndex == -1) {
			return false;
		}
		String httpStr = requestLine.substring(spaceIndex+1);
		if(!httpStr.equalsIgnoreCase(HTTPConstants.HTTP10) &&
		   !httpStr.equalsIgnoreCase(HTTPConstants.HTTP11)) {
			return false;
		}
		return true;
	}	
}

