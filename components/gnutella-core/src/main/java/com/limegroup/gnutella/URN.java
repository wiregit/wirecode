package com.limegroup.gnutella;

import com.bitzi.util.*;
import java.io.*;
import java.security.*;

/**
 * This class represents an individual Uniform Resource Name (URN), as
 * specified in RFC 2141.  This also provides public constants for 
 * standard URN strings and URN strings that have been agreed upon by
 * the Gnutella Developers Forum (GDF).<p>
 *
 * This class is immutable.
 */
public final class URN implements HTTPHeaderValue {

	/**
	 * Constant for the leading URN string identifier, as specified in
	 * RFC 2141.  This is equal to "urn:", although note that this
	 * should be used in a case-insensitive manner in compliance with
	 * the URN specification (RFC 2141).
	 */
	private static final String URN_NS_ID = "urn:";

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
	public static final String URN_SHA1 = URN_NS_ID+SHA1;

	/**
	 * Constant code for a SHA1 URN.
	 */
	public static final int SHA1_URN = 100;

	/**
	 * The string representation of the URN.
	 */
	private final String URN_STRING;

	/**
	 * Cached hash code that is lazily initialized.
	 */
	private volatile int hashCode = 0;  

	/**
	 * Constructs a new URN based on the specified <tt>File</tt> instance.
	 * The constructor calculates the SHA1 value for the file, and is a
	 * costly operation as a result.
	 *
	 * @param file the <tt>File</tt> instance to construct the URN from
	 * @param urnType the type of URN to construct for the <tt>File</tt>
	 *  instance, such as SHA1_URN
	 * @throws <tt>IllegalArgumentException</tt> if the URN type specified
	 *  is invalid
	 */
	public URN(final File file, final int urnType) throws IOException {
		switch(urnType) {
		case SHA1_URN:
			this.URN_STRING = URN.createSHA1String(file);
			break;
		default:
			throw new IllegalArgumentException("INVALID URN TYPE SPECIFIED");
		}
	}

	/**
	 * Constructs a new <tt>URN</tt> instance from the specified string
	 * representation of a URN.  
	 *
	 * @param urnString the URN string to use for constructing this URN 
	 *  instance
	 * @throws <tt>IOException</tt> if the supplies URN string is not valid
	 */
	public URN(final String urnString) throws IOException {
		if(!URN.isValidUrn(urnString)) {
			throw new IOException("INVALID URN STRING");
		}
		this.URN_STRING = urnString;
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
		return URN_STRING.substring(0,URN_STRING.indexOf(':',4)+1);		
	}

	public String httpStringValue() {
		return URN_STRING;
	}

	/**
	 * Returns whether or not the URN_STRING argument is a valid URN 
	 * string, as specified in RFC 2141.
	 *
	 * @param urnString the urn string to check for validity
	 * @return <tt>true</tt> if the string argument is a URN, 
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isUrn(final String urnString) {
		return URN.isValidUrn(urnString);
	}

	/**
	 * Returns whether or not the string argument is a urn type that
	 * we know about.
	 *
	 * @param urnString to string to check 
	 * @return <tt>true</tt> if it is a valid URN type, <tt>false</tt>
	 *  otherwise
	 */
	public static boolean isUrnType(String urnString) {
		final String urnStringLower = urnString.toLowerCase();
		if(urnStringLower.equals(URN.URN_NS_ID) || 
		   urnStringLower.equals(URN.URN_SHA1 + ":")) {
			return true;
		}
		return false;
	}

	/**
	 * Create a new SHA1 hash string for the specified file on disk.
	 *
	 * @param file the file to construct the hash from
	 * @return the SHA1 hash string
	 * @throws <tt>IOException</tt> if there is an error creating the hash
	 * @throws <tt>NoSuchAlgorithmException</tt> if the specified algorithm
	 *  cannot be found
	 */
	private static String createSHA1String(final File file) 
		throws IOException {
		FileInputStream fis = new FileInputStream(file);   		
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
	 * Returns whether or not the specified string represents a valid 
	 * URN.  For a full description of what qualifies as a valid URN, 
	 * see RFC2141 ( http://www.ietf.org ).<p>
	 *
	 * The broad requirements of the URN are that it meet the following 
	 * syntax: <p>
	 *
	 * <URN> ::= "urn:" <NID> ":" <NSS>  <p>
	 * 
	 * where phrases enclosed in quotes are required and where "<NID>" is the
	 * Namespace Identifier and "<NSS>" is the Namespace Specific String.
	 *
	 * @param urnString the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the specified string represents a valid urn,
	 *         <tt>false</tt> otherwise
	 */
	private static boolean isValidUrn(final String urnString) {
		int colon1Index = urnString.indexOf(":");
		if(colon1Index == -1) {
			return false;
		}

		int urnIndex1 = colon1Index-3;
		int urnIndex2 = colon1Index+1;

		if((urnIndex1 < 0) || (urnIndex2 < 0)) {
			return false;
		}

		// get the "urn:" substring so we can make sure it's there,
		// ignoring case
		String urnStr = urnString.substring(0, colon1Index+1);

		// get the last colon -- this should separate the <NID>
		// from the <NIS>
		int colon2Index = urnString.indexOf(":", colon1Index+1);
		
		if((colon2Index == -1) || 
		   !urnStr.equalsIgnoreCase(URN.URN_NS_ID) ||
		   !isValidNamespaceIdentifier(urnString.substring(colon1Index+1, 
														   colon2Index)) ||
		   !isValidNamespaceSpecificString(urnString.substring(colon2Index+1))) {
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
	private static boolean isValidNamespaceIdentifier(final String NID) {					
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
	private static boolean isValidNamespaceSpecificString(final String NSS) {
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
	public String stringValue() {
		return URN_STRING;
	}

	/**
	 * Returns whether or not this URN is a SHA1 URN.  Note that a bitprint
	 * URN will return false, even though it contains a SHA1 hash.
	 *
	 * @return <tt>true</tt> if this is a SHA1 URN, <tt>false</tt> otherwise
	 */
	public boolean isSHA1() {
		return URN_STRING.toLowerCase().startsWith(URN_SHA1);
	}

	/**
	 * Checks for URN equality.  For URNs to be equal, their URN strings must
	 * be equal.
	 *
	 * @param o the object to compare against
	 * @return <tt>true</tt> if the URNs are equal, <tt>false</tt> otherwise
	 */
	public boolean equals(Object o) {
		if(o == this) return true;
		if(!(o instanceof URN)) {
			return false;
		}
		URN urn = (URN)o;
		
		return (URN_STRING == null ? urn.URN_STRING == null : 
				URN_STRING.equals(urn.URN_STRING));
	}

	/**
	 * Overrides the hashCode method of Object to meet the contract of 
	 * hashCode.  Since we override equals, it is necessary to also 
	 * override hashcode to ensure that two "equal" instances of this
	 * class return the same hashCode, less we unleash unknown havoc on 
	 * the hash-based collections.
	 *
	 * @return a hash code value for this object
	 */
	public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			result = (37*result) + this.URN_STRING.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	/**
	 * Overrides toString to return the URN string.
	 *
	 * @return the string representation of the URN
	 */
	public String toString() {
		return URN_STRING;
	}
}
