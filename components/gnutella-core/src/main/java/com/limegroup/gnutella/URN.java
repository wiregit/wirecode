package com.limegroup.gnutella;

import com.bitzi.util.*;
import java.io.*;
import java.security.*;

/**
 * This class represents an individual Uniform Resource Name (URN), as
 * specified in RFC 2141.  This also provides public constants for 
 * standard URN strings and URN strings that have been agreed upon by
 * the Gnutella Developers Forum (GDF).
 */
public final class URN {

	/**
	 * Constant for the leading URN string identifier, as specified in
	 * RFC 2141.  This is equal to "urn:", although note that this
	 * should be used in a case-insensitive manner in compliance with
	 * the URN specification (RFC 2141).
	 */
	private static final String URN_STRING = "urn:";

	/**
	 * The identifier for a SHA1 Namespace Identifier string.  This
	 * is "sha1" and should also be used in a case-insensitive manner.
	 */
	private static final String SHA1 = "sha1";

	/**
	 * Constant for the bitprint Namespace Identifier string.  This 
	 * is "bitprint" and should be used in a case-insensitive manner.
	 */
	private static final String BITPRINT = "bitprint";
	
	/**
	 * Constant for the URN identifier constant combined with the
	 * SHA1 identifier constant.  This is used to identify SHA1 URNs.
	 */
	public static final String URN_SHA1 = URN_STRING+SHA1;

	/**
	 * Constant code for a SHA1 URN.
	 */
	public static final int SHA1_URN = 100;


	/**
	 * The string representation of the URN.
	 */
	private String _urnString;
  

	/**
	 * Constructs a new URN based on the specified <tt>File</tt> instance.
	 * The constructor calculates the SHA1 value for the file, and is a
	 * costly operation as a result.
	 *
	 * @param file the <tt>File</tt> instance to construct the URN from
	 * @param URN_TYPE the type of URN to construct for the <tt>File</tt>
	 *  instance, such as SHA1_URN
	 * @throws <tt>IllegalArgumentException</tt> if the URN type specified
	 *  is invalid
	 */
	public URN(final File FILE, final int URN_TYPE) throws IOException {
		switch(URN_TYPE) {
		case SHA1_URN:
			_urnString = createSHA1String(FILE);
		default:
			throw new IllegalArgumentException("INVALID URN TYPE SPECIFIED");
		}
	}

	/**
	 * Constructs a new <tt>URN</tt> instance from the specified string
	 * representation of a URN.  
	 *
	 * @param URN_STRING the URN string to use for constructing this URN 
	 *  instance
	 * @throws <tt>IOException</tt> if the supplies URN string is not valid
	 */
	public URN(final String URN_STRING) throws IOException {
		if(!URN.isValidURN(URN_STRING)) {
			throw new IOException("INVALID URN STRING");
		}
		_urnString = URN_STRING;
	}

	/**
	 * Returns whether or not the URN_STRING argument is a valid URN 
	 * string, as specified in RFC 2141.
	 *
	 * @param URN_STRING the urn string to check for validity
	 * @return <tt>true</tt> if the string argument is a URN, 
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isURN(final String URN_STRING) {
		return URN.isValidURN(URN_STRING);
	}

	/**
	 * Returns whether or not the string argument is a urn type that
	 * we know about.
	 *
	 * @param urnString to string to check 
	 * @return <tt>true</tt> if it is a valid URN type, <tt>false</tt>
	 *  otherwise
	 */
	public static boolean isURNType(String urnString) {
		final String URN_STRING = urnString.toLowerCase();
		if(URN_STRING.equals(URN.URN_STRING) || 
		   URN_STRING.equals(URN.URN_SHA1 + ";")) {
			return true;
		}
		return false;
	}

	/**
	 * Returns the URN type string for this URN.  For example, if the
	 * String for this URN is:<p>
	 * 
	 * urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB <p>
	 *
	 * then this method will return: <p>
	 *
	 * urn:sha1:
	 */
	public String getTypeString() {
		return _urnString.substring(0,_urnString.indexOf(':',4)+1);		
	}

	/**
	 * Create a new SHA1 hash string for the specified file on disk.
	 *
	 * @param FILE the file to construct the hash from
	 * @return the SHA1 hash string
	 * @throws <tt>IOException</tt> if there is an error creating the hash
	 * @throws <tt>NoSuchAlgorithmException</tt> if the specified algorithm
	 *  cannot be found
	 */
	private static String createSHA1String(final File FILE) 
		throws IOException {
		FileInputStream fis = new FileInputStream(FILE);   		
		// we can only calculate SHA1 for now
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA");
		} catch(NoSuchAlgorithmException e) {
			throw new IOException("NO SUCH ALGORITHM");
		}
		byte[] buffer = new byte[16384];
		int read;
		while ((read=fis.read(buffer))!=-1) {
			md.update(buffer,0,read);
		}
		fis.close();
		byte[] sha1 = md.digest();

		// preferred casing: lowercase "urn:sha1:", uppercase encoded value
		// note that all URNs are case-insensitive for the "urn:<type>:" part,
		// but some MAY be case-sensitive thereafter (SHA1/Base32 is case 
		// insensitive)
		return URN_SHA1 + ":" + Base32.encode(sha1);
	}

	/**
	 * Returns a <tt>String</tt> containing the URN for the get request.  For
	 * a typical SHA1 request, this will return a 41 character URN, including
	 * the 32 character hash value.  For a full description of what qualifies
	 * as a valid URN, see RFC2141 ( http://www.ietf.org ).<p>
	 *
	 * The broad requirements of the URN are that it meet the following 
	 * syntax: <p>
	 *
	 * <URN> ::= "urn:" <NID> ":" <NSS>  <p>
	 * 
	 * where phrases enclosed in quotes are required and where "<NID>" is the
	 * Namespace Identifier and "<NSS>" is the Namespace Specific String.
	 *
	 * @param URN_STRING the <tt>String</tt> instance containing the get request
	 * @return a <tt>String</tt> containing the URN for the get request
	 * @throws <tt>IOException</tt> if there is an error parsing out the URN
	 *  from the line
	 */
	private static boolean isValidURN(final String URN_STRING) {
		int colon1Index = URN_STRING.indexOf(":");
		if(colon1Index == -1) {
			return false;
		}

		// get the "urn:" substring so we can make sure it's there,
		// ignoring case
		String urnStr = URN_STRING.substring(colon1Index-3, colon1Index+1);

		// get the last colon -- this should separate the <NID>
		// from the <NIS>
		int colon2Index = URN_STRING.indexOf(":", colon1Index+1);
		
		if((colon2Index == -1) || 
		   !urnStr.equalsIgnoreCase(URN_STRING) ||
		   !isValidNID(URN_STRING.substring(colon1Index+1, colon2Index)) ||
		   !isValidNSS(URN_STRING.substring(colon2Index+1))) {
			return false;
		}		
		return true;
	}

	/**
	 * Returns whether or not the specified Namespace Identifier String (NID) 
	 * is a valid NID.
	 *
	 * @param NID the Namespace Identifier String for a URN
	 * @return <tt>true</tt> if the NID is valid, <tt>false</tt> otherwise
	 */
	private static boolean isValidNID(final String NID) {					
		// we should add other namespace identifiers to this check as
		// they become registered
		if(!NID.equalsIgnoreCase(URN.SHA1) &&
		   !NID.equalsIgnoreCase(URN.BITPRINT)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the specified Namespace Specific String (NSS) 
	 * is a valid NSS.
	 *
	 * @param NSS the Namespace Specific String for a URN
	 * @return <tt>true</tt> if the NSS is valid, <tt>false</tt> otherwise
	 */
	private static boolean isValidNSS(final String NSS) {
		int length = NSS.length();

		// checks to make sure that it either is the length of a 32 
		// character SHA1 NSS, or is the length of a 72 character
		// bitprint NSS
		if((length != 32) && (length != 72)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the string representation of the URN.  This is in the form:<p>
	 * 
	 * <URN> ::= "urn:" <NID> ":" <NSS> <p>
	 * 
	 * as outlined in RFC 2141.
	 *
	 * @return the string representation of the URN, or <tt>null</tt> if none
	 *  has been assigned
	 */
	public String getURNString() {
		return _urnString;
	}

	/**
	 * Returns whether or not this URN is a SHA1 URN.  Note that a bitprint
	 * URN will return false, even though it contains a SHA1 hash.
	 *
	 * @return <tt>true</tt> if this is a SHA1 URN, <tt>false</tt> otherwise
	 */
	public boolean isSHA1() {
		return _urnString.startsWith(URN_SHA1);
	}

	/**
	 * Checks for URN equality.  For URNs to be equal, their URN strings must
	 * be equal.
	 *
	 * @param urn the URN to compare against
	 * @return <tt>true</tt> if the URNs are equal, <tt>false</tt> otherwise
	 */
	public boolean equals(Object urn) {
		if(!(urn instanceof URN)){
			return false;
		}
		return ((URN)urn).getURNString().equals(_urnString);
	}

	/**
	 * Overrides toString to return the URN string.
	 *
	 * @return the string representation of the URN
	 */
	public String toString() {
		return _urnString;
	}
}
