package com.limegroup.gnutella;

import com.limegroup.gnutella.http.*; 
import com.bitzi.util.*;
import java.io.*;
import java.security.*;

/**
 * This class represents an individual Uniform Resource Name (URN), as
 * specified in RFC 2141.  This does extensive validation of URNs to 
 * make sure that they are valid, with the factory methods throwing 
 * excpeptions when the arguments do not meet URN syntax.  This does
 * not perform rigorous verification of the SHA1 values themselves.
 *
 * This class is immutable.
 *
 * @see UrnCache
 * @see FileDesc
 * @see UrnType
 * @see java.io.Serializable
 */
public final class URN implements HTTPHeaderValue, Serializable {

	private static final long serialVersionUID = -6053855548211564799L;

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
	 * The string representation of the URN.
	 */
	private transient String _urnString;

	/**
	 * Variable for the <tt>UrnType</tt> instance for this URN.
	 */
	private transient UrnType _urnType;

	/**
	 * Cached hash code that is lazily initialized.
	 */
	private volatile transient int hashCode = 0;  

	/**
	 * Creates a new <tt>URN</tt> instance with a SHA1 hash.
	 *
	 * @param file the <tt>File</tt> instance to use to create a 
	 *  <tt>URN</tt>
	 * @return a new <tt>URN</tt> instance
	 * @throws <tt>IOException</tt> if there was an error constructing
	 *  the <tt>URN</tt>
     * @throws <tt>InterruptedException</tt> if the calling thread was 
     *  interrupted while hashing.  (This method can take a while to
     *  execute.)
	 */
	public static URN createSHA1Urn(File file) 
		throws IOException, InterruptedException {
		return new URN(createSHA1String(file), UrnType.SHA1);
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
		return createSHA1UrnFromString(urnString);
	}

	/**
	 * Creates a URN instance from the specified HTTP request line.
	 * The request must be in the standard from, as specified in
	 * RFC 2169.  Note that com.limegroup.gnutella.Acceptor parses out
	 * the first word in the request, such as "GET" or "HEAD."
	 *
	 * @param requestLine the URN HTTP request of the form specified in
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
		if(!URN.isValidUrnHttpRequest(requestLine)) {
			throw new IOException("IVVALID URN HTTP REQUEST");
		}
		String urnString = URN.extractUrnFromHttpRequest(requestLine);
		if(urnString == null) {
			throw new IOException("COULD NOT CONSTRUCT URN");
		}	   
		return createSHA1UrnFromString(urnString);
	}

	/**
	 * Convenience method that runs a standard validation check on the URN
	 * string before calling the <tt>URN</tt> constructor.
	 *
	 * @param urnString the string for the urn
	 * @return a new <tt>URN</tt> built from the specified string
	 * @throws <tt>IOException</tt> if there is an error
	 */
	private static URN createSHA1UrnFromString(final String urnString) 
		throws IOException {
		if(!URN.isValidUrn(urnString)) {
			throw new IOException("invalid urn string: "+urnString);
		}
		String typeString = URN.getTypeString(urnString);
		if(!UrnType.isSupportedUrnType(typeString)) {
			throw new IOException("urn type not recognized: "+typeString);
		}
		UrnType type = UrnType.createUrnType(typeString);
		return new URN(urnString, type);
	}

	/**
	 * Constructs a new URN based on the specified <tt>File</tt> instance.
	 * The constructor calculates the SHA1 value for the file, and is a
	 * costly operation as a result.
	 *
	 * @param file the <tt>File</tt> instance to construct the URN from
	 * @param urnType the type of URN to construct for the <tt>File</tt>
	 *  instance, such as SHA1_URN
	 * @throws <tt>IOException</tt> if the URN could not be calculated from
	 *  the specified file
	 */
	private URN(final String urnString, final UrnType urnType) throws IOException {
		this._urnString = urnString;
		this._urnType = urnType;
	}

	/**
	 * Returns the <tt>UrnType</tt> instance for this <tt>URN</tt>.
	 *
	 * @return the <tt>UrnType</tt> instance for this <tt>URN</tt>
	 */
	public UrnType getUrnType() {
		return _urnType;
	}

	// implements HTTPHeaderValue
	public String httpStringValue() {
		return _urnString;
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
	 * Returns whether or not this URN is a SHA1 URN.  Note that a bitprint
	 * URN will return false, even though it contains a SHA1 hash.
	 *
	 * @return <tt>true</tt> if this is a SHA1 URN, <tt>false</tt> otherwise
	 */
	public boolean isSHA1() {
		return _urnType.isSHA1();
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
		
		return (_urnString.equals(urn._urnString) &&
				_urnType.equals(urn._urnType));
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
			result = (37*result) + this._urnString.hashCode();
			result = (37*result) + this._urnType.hashCode();
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
		return _urnString;
	}

	/**
	 * Returns a <tt>String</tt> containing the URN for the http request.  For
	 * a typical SHA1 request, this will return a 41 character URN, including
	 * the 32 character hash value.
	 *
	 * @param requestLine the <tt>String</tt> instance containing the request
	 * @return a <tt>String</tt> containing the URN for the http request, or 
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
	 * Returns whether or not the http request is valid, as specified in
	 * HUGE v. 0.93 and IETF RFC 2169.  This verifies everything except
	 * whether or not the URN itself is valid -- the URN constructor
	 * can do that, however.
	 *
	 * @param requestLine the <tt>String</tt> instance containing the http 
	 *  request
	 * @return <tt>true</tt> if the reques is valid, <tt>false</tt> otherwise
	 */
	private static boolean isValidUrnHttpRequest(final String requestLine) {
		return (URN.isValidSize(requestLine) &&
				URN.isValidUriRes(requestLine) &&
				URN.isValidResolutionProtocol(requestLine) && 
				URN.isValidHTTPSpecifier(requestLine));				
	}

	/** 
	 * Returns whether or not the specified http request meets size 
	 * requirements.
	 *
	 * @param requestLine the <tt>String</tt> instance containing the http request
	 * @return <tt>true</tt> if the size of the request line is valid, 
	 *  <tt>false</tt> otherwise
	 */
	private static final boolean isValidSize(final String requestLine) {
		int size = requestLine.length();
		if((size != 63) && (size != 107)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the http request corresponds with the standard 
	 * uri-res request
	 *
	 * @param requestLine the <tt>String</tt> instance containing the http request
	 * @return <tt>true</tt> if the http request includes the standard "uri-res"
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
	 * Returns whether or not the "resolution protocol" for the given URN http
	 * line is valid.  We currently only support N2R, which specifies "Given 
	 * a URN, return the named resource."
	 *
	 * @param requestLine the <tt>String</tt> instance containing the request
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
	 * Returns whether or not the HTTP specifier for the URN http request
	 * is valid.
	 *
	 * @param requestLine the <tt>String</tt> instance containing the http request
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

	/**
	 * Returns the URN type string for this URN.  This requires that each URN 
	 * have a specific type - a general "urn:" type is not accepted.  As an example
	 * of how this method behaves, if the string for this URN is:<p>
	 * 
	 * urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB <p>
	 *
	 * then this method will return: <p>
	 *
	 * urn:sha1:
	 *
	 * @param fullUrnString the string containing the full urn
	 * @return the urn type of the string
	 */
	private static String getTypeString(final String fullUrnString) {		
		// trims any leading whitespace from the urn string -- without 
		// whitespace the urn must start with 'urn:'
		String type = fullUrnString.trim();
		return type.substring(0,type.indexOf(':', 4)+1); 
	}

	/**
	 * Create a new SHA1 hash string for the specified file on disk.
	 *
	 * @param file the file to construct the hash from
	 * @return the SHA1 hash string
	 * @throws <tt>IOException</tt> if there is an error creating the hash
	 *  or if the specified algorithm cannot be found
     * @throws <tt>InterruptedException</tt> if the calling thread was 
     *  interrupted while hashing.  (This method can take a while to
     *  execute.)
	 */
	private static String createSHA1String(final File file) 
		    throws IOException, InterruptedException {
		FileInputStream fis = new FileInputStream(file);   		
		// we can only calculate SHA1 for now
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA");
		} catch(NoSuchAlgorithmException e) {
			throw new IOException("NO SUCH ALGORITHM");
		}
        
        try {
            byte[] buffer = new byte[16384];
            int read;
            while ((read=fis.read(buffer))!=-1) {
                long start = System.currentTimeMillis();
                md.update(buffer,0,read);
                long end = System.currentTimeMillis();
                long interval = Math.max(0, end-start);   //ensure non-negative
                Thread.sleep(interval*2);                 //throws InterruptedException 
            }
        } finally {		
            fis.close();
        }

		byte[] sha1 = md.digest();

		// preferred casing: lowercase "urn:sha1:", uppercase encoded value
		// note that all URNs are case-insensitive for the "urn:<type>:" part,
		// but some MAY be case-sensitive thereafter (SHA1/Base32 is case 
		// insensitive)
		return UrnType.URN_NAMESPACE_ID+UrnType.SHA1_STRING+Base32.encode(sha1);
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
	 * @param urnString the <tt>String</tt> instance containing the http request
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
		if(colon2Index == -1) return false;
		String urnType = urnString.substring(0, colon2Index+1);
		if(!UrnType.isSupportedUrnType(urnType) ||
		   !isValidNamespaceSpecificString(urnString.substring(colon2Index+1))) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the specified Namespace Specific String (NSS) 
	 * is a valid NSS.
	 *
	 * @param nss the Namespace Specific String for a URN
	 * @return <tt>true</tt> if the NSS is valid, <tt>false</tt> otherwise
	 */
	private static boolean isValidNamespaceSpecificString(final String nss) {
		int length = nss.length();

		// checks to make sure that it either is the length of a 32 
		// character SHA1 NSS, or is the length of a 72 character
		// bitprint NSS
		if((length != 32) && (length != 72)) {
			return false;
		}
		return true;
	}

	/**
	 * Serializes this instance.
	 *
	 * @serialData the string representation of the URN
	 */
	private void writeObject(ObjectOutputStream s) 
		throws IOException {
		s.defaultWriteObject();
		s.writeUTF(_urnString);
		s.writeObject(_urnType);
	}

	/**
	 * Deserializes this <tt>URN</tt> instance, validating the urn string
	 * to ensure that it's valid.
	 */
	private void readObject(ObjectInputStream s) 
		throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		_urnString = s.readUTF();
		_urnType = (UrnType)s.readObject();
		if(!URN.isValidUrn(_urnString)) {
			throw new InvalidObjectException("invalid urn: "+_urnString);
		}
		if(_urnType.isSHA1()) {
			// this preserves instance equality for all SHA1 run types
			_urnType = UrnType.SHA1;
		}
		else {
			throw new InvalidObjectException("invalid urn type: "+_urnType);
		}		
	}
}
