package com.limegroup.gnutella;

import java.io.*;

/**
 * This class is a factory for creating new <tt>URN</tt> instances.  It can
 * create <tt>URN</tt>s containing SHA1 hashes, for example.  It can also
 * do so from an HTTP get request line that includes a URN as long as the
 * request is in the format specified in RFC 2169.
 */
public final class URNFactory {

	/**
	 * Creates a new <tt>URN</tt> instance with a SHA1 hash.
	 *
	 * @param file the <tt>File</tt> instance to use to create a 
	 *  <tt>URN</tt>
	 * @return a new <tt>URN</tt> instance
	 * @throws <tt>IOException</tt> if there was an error constructing
	 *  the <tt>URN</tt>
	 */
	public static URN createSHA1URN(File file) 
		throws IOException {
		return new URN(file, URN.SHA1_URN);
	}

	/**
	 * Creates a new <tt>URN</tt> instance from the specified string.
	 * The resulting URN can had any Namespace Identifier and any
	 * Namespace Specific String.
	 *
	 * @param URN_STRING the string instance to use to create a 
	 *  <tt>URN</tt>
	 * @return a new <tt>URN</tt> instance
	 * @throws <tt>IOException</tt> if there was an error constructing
	 *  the <tt>URN</tt>
	 */
	public static URN createURN(final String URN_STRING) 
		throws IOException {
		return new URN(URN_STRING);
	}

	/**
	 * Creates a URN instance from the specified HTTP get request line.
	 * The get request must be in the standard from, as specified in
	 * RFC 2169.
	 *
	 * @param GET_LINE the URN HTTP GET request of the form specified in
	 *  RFC 2169, for example:<p>
	 * 
	 * 	GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1
	 * @return a new <tt>URN</tt> instance from the specified request, or 
	 *  <tt>null</tt> if no <tt>URN</tt> could be created
	 */
	public static URN createSHA1URNFromGetRequest(final String GET_LINE) 
		throws IOException {
		if(!URNFactory.isValidURNGetRequest(GET_LINE)) {
			throw new IOException("INVALID URN GET REQUEST");
		}
		String urnStr = URNFactory.getURNFromGetRequest(GET_LINE);
		if(urnStr == null) {
			throw new IOException("COULD NOT CONSTRUCT URN");
		}
		
		// this constructor can also throw an IOException
		return new URN(urnStr);
	}

	/**
	 * Returns a <tt>String</tt> containing the URN for the get request.  For
	 * a typical SHA1 request, this will return a 41 character URN, including
	 * the 32 character hash value.
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return a <tt>String</tt> containing the URN for the get request, or 
	 *  <tt>null</tt> if the request could not be read
	 */
	private static String getURNFromGetRequest(final String GET_LINE) {
		int qIndex     = GET_LINE.indexOf("?") + 1;
		int spaceIndex = GET_LINE.indexOf(" ", qIndex);		
		if((qIndex == -1) || (spaceIndex == -1)) {
			return null;
		}
		return GET_LINE.substring(qIndex, spaceIndex);
	}

	/**
	 * Returns whether or not the get request is valid, as specified in
	 * HUGE v. 0.93 and IETF RFC 2169.  This verifies everything except
	 * whether or not the URN itself is valid -- the URN constructor
	 * can do that, however.
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the reques is valid, <tt>false</tt> otherwise
	 */
	private static boolean isValidURNGetRequest(final String GET_LINE) {
		return (URNFactory.isValidSize(GET_LINE) &&
				URNFactory.isValidGet(GET_LINE) &&
				URNFactory.isValidUriRes(GET_LINE) &&
				URNFactory.isValidResolutionProtocol(GET_LINE) && 
				URNFactory.isValidHTTPSpecifier(GET_LINE));				
	}

	/** 
	 * Returns whether or not the specified get request meets size 
	 * requirements.
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the get request starts with "GET "
	 *  (case-insensitive), <tt>false</tt> otherwise
	 */
	private static final boolean isValidSize(final String GET_LINE) {
		int size = GET_LINE.length();
		if((size != 67) && (size != 111)) {
			return false;
		}
		return true;
	}


	/**
	 * Returns whether or not the get request corresponds with the standard 
	 * start of a get request.
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the get request starts with "GET "
	 *  (case-insensitive), <tt>false</tt> otherwise
	 */
	private static final boolean isValidGet(final String GET_LINE) {
		int firstSpace = GET_LINE.indexOf(" ");
		if(firstSpace == -1) {
			return false;
		}
		String getStr = GET_LINE.substring(0, firstSpace);
		if(!getStr.equalsIgnoreCase(HTTPConstants.GET)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the get request corresponds with the standard 
	 * uri-res request
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the get request includes the standard "uri-res"
	 *  (case-insensitive) request, <tt>false</tt> otherwise
	 */
	private static final boolean isValidUriRes(final String GET_LINE) {
		int firstSlash = GET_LINE.indexOf("/");
		if(firstSlash == -1) {
			return false;
		}
		int secondSlash = GET_LINE.indexOf("/", firstSlash+1);
		if(secondSlash == -1) {
			return false;
		}
		String uriStr = GET_LINE.substring(firstSlash+1, secondSlash);
		if(!uriStr.equalsIgnoreCase(HTTPConstants.URI_RES)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the specified "resolution protocol" is valid.
	 * We currently only support N2R, which specifies "Given a URN, return the
	 * named resource."
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the resolution protocol is valid, <tt>false</tt>
	 *  otherwise
	 */
	private static boolean isValidResolutionProtocol(final String GET_LINE) {
		int nIndex = GET_LINE.indexOf("2");
		if(nIndex == -1) {
			return false;
		}
		String n2r = GET_LINE.substring(nIndex-1, nIndex+3);

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
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the HTTP specifier is valid, <tt>false</tt>
	 *  otherwise
	 */
	private static boolean isValidHTTPSpecifier(final String GET_LINE) {
		int spaceIndex = GET_LINE.lastIndexOf(" ");
		if(spaceIndex == -1) {
			return false;
		}
		String httpStr = GET_LINE.substring(spaceIndex+1);
		if(!httpStr.equalsIgnoreCase(HTTPConstants.HTTP10) &&
		   !httpStr.equalsIgnoreCase(HTTPConstants.HTTP11)) {
			return false;
		}
		return true;
	}

	// unit test for the validating of GET requests
	public static void main(String[] args) {
		String [] validURNS = {
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /URI-RES/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/n2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2r?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/n2r?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?UrN:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sHa1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HtTP/1.0",
		    "GET /uri-res/N2R?urn:bitprint:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
			"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.0",
		    "GET /uri-res/N2R?urn:bitprint:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
			"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.1"
		};

		String [] invalidURNS = {
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.2",
		    "GET /urires/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		    "/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJcdirnZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.2",
		    "GET /uri-res/N2Rurn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sh1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		    "GET/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:bitprint::PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
			"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1::PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
			"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPF HTTP/1.0",
		    "GET /uri-res/N2R?ur:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R? urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?  urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?                                                    "+
			"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1: PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/ N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2Rurn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urnsha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sa1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0 ",
		    " GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0 ",
			" ",
			"GET",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFBC HTTP/1.0",
		};
		//UploadManager um = new UploadManager();
		boolean encounteredFailure = false;
		System.out.println("TESTING THAT VALID URN GET REQUESTS PASS..."); 
		for(int i=0; i<validURNS.length; i++) {
			if(URNFactory.isValidURNGetRequest(validURNS[i]) != true) {
				if(!encounteredFailure) {
					System.out.println(  ); 
					System.out.println("VALID URN TEST FAILED");
				}
				encounteredFailure = true;
				System.out.println(); 
				System.out.println("FAILED ON URN: ");
				System.out.println(validURNS[i]); 
			}
		}
		if(!encounteredFailure) {
			System.out.println("TEST PASSED"); 
		}
		System.out.println(); 
		System.out.println("TESTING THAT INVALID URN GET REQUESTS FAIL..."); 
		for(int i=0; i<invalidURNS.length; i++) {
			if(URNFactory.isValidURNGetRequest(invalidURNS[i]) == true) {
				if(!encounteredFailure) {
					System.out.println("INVALID URN TEST FAILED");
				}
				encounteredFailure = true;
				System.out.println(); 
				System.out.println("FAILED ON URN "+i+":");
				System.out.println(invalidURNS[i]); 
			}			
		}
		if(!encounteredFailure) {
			System.out.println("TEST PASSED"); 
		}
		System.out.println(); 
		if(!encounteredFailure) {
			System.out.println("ALL TESTS PASSED"); 
		}
	}
	
}
