package com.limegroup.gnutella;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.security.SHA1;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.IntWrapper;
import com.limegroup.gnutella.util.SystemUtils;

/**
 * This class represents an individual Uniform Resource Name (URN), as
 * specified in RFC 2141.  This does extensive validation of URNs to 
 * make sure that they are valid, with the factory methods throwing 
 * exceptions when the arguments do not meet URN syntax.  This does
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
    
    /** An empty set casted to URNs. */
    public static final Set<URN> NO_URN_SET = Collections.emptySet();
	
	/**
	 * A constant invalid URN that classes can use to represent an invalid URN.
	 */
	public static final URN INVALID = new URN("bad:bad", UrnType.INVALID);
	
	/**
	 * The amount of time we must be idle before we start
	 * devoting all processing time to hashing.
	 * (Currently 5 minutes).
	 */
	private static final int MIN_IDLE_TIME = 5 * 60 * 1000;

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
     * Cached constant to avoid making unnecessary string allocations
     * in validating input.
     */
    private static final String DOT = ".";

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
	 * The progress of files currently being hashed.
	 * Files are added to this when hashing is started
	 * and removed when hashing finishes.
	 * IntWrapper stores the amount of bytes read.
	 */
	private static final Map<File, IntWrapper> progressMap =
	    Collections.synchronizedMap(new HashMap<File, IntWrapper>());
	
	/**
	 * Gets the amount of bytes hashed for a file that is being hashed.
	 * Returns -1 if the file is not being hashed at all.
	 */
	public static int getHashingProgress(File file) {
	    IntWrapper progress = progressMap.get(file);
	    if ( progress == null )
	        return -1;
	    else
	        return progress.getInt();
	}

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
	 * The resulting URN can have any Namespace Identifier and any
	 * Namespace Specific String.
	 *
	 * @param urnString a string description of the URN.  Typically 
     *  this will be a SHA1 containing a 32-character value, e.g., 
     *  "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB".
	 * @return a new <tt>URN</tt> instance
	 * @throws <tt>IOException</tt> urnString was malformed or an
     *  unsupported type
	 */
	public static URN createSHA1Urn(final String urnString) 
		throws IOException {
        String typeString = URN.getTypeString(urnString).toLowerCase(Locale.US);
        if (typeString.indexOf(UrnType.SHA1_STRING) == 4)
    		return createSHA1UrnFromString(urnString);
        else if (typeString.indexOf(UrnType.BITPRINT_STRING) == 4)
            return createSHA1UrnFromBitprint(urnString);
        else
            throw new IOException("unsupported or malformed URN");
	}
	
	/**
	 * Retrieves the TigerTree Root hash from a bitprint string.
	 */
	public static String getTigerTreeRoot(final String urnString) throws IOException {
        String typeString = URN.getTypeString(urnString).toLowerCase(Locale.US);
        if (typeString.indexOf(UrnType.BITPRINT_STRING) == 4)
            return getTTRootFromBitprint(urnString);
        else
            throw new IOException("unsupported or malformed URN");
    }
	    

	/**
	 * Convenience method for creating a SHA1 <tt>URN</tt> from a <tt>URL</tt>.
	 * For the url to work, its getFile method must return the SHA1 urn
	 * in the form:<p> 
	 * 
	 *  /uri-res/N2R?urn:sha1:SHA1URNHERE
	 * 
	 * @param url the <tt>URL</tt> to extract the <tt>URN</tt> from
	 * @throws <tt>IOException</tt> if there is an error reading the URN from
	 *  the URL
	 */
	public static URN createSHA1UrnFromURL(final URL url) 
		throws IOException {
		return createSHA1UrnFromUriRes(url.getFile());
	}

	/**
	 * Convenience method for creating a <tt>URN</tt> instance from a string
	 * in the form:<p>
	 *
	 * /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB
	 */
	public static URN createSHA1UrnFromUriRes(String sha1String) 
		throws IOException {
		sha1String.trim();
		if(isValidUriResSHA1Format(sha1String)) {
			return createSHA1UrnFromString(sha1String.substring(13));
		} else {
			throw new IOException("could not parse string format: "+sha1String);
		}
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
	 *  /uri-res/N2X?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1
	 *  /uri-res/N2R?urn:bitprint:QLFYWY2RI5WZCTEP6MJKR5CAFGP7FQ5X.VEKXTRSJPTZJLY2IKG5FQ2TCXK26SECFPP4DX7I HTTP/1.1
     *  /uri-res/N2X?urn:bitprint:QLFYWY2RI5WZCTEP6MJKR5CAFGP7FQ5X.VEKXTRSJPTZJLY2IKG5FQ2TCXK26SECFPP4DX7I HTTP/1.1	 
	 *
	 * @return a new <tt>URN</tt> instance from the specified request, or 
	 *  <tt>null</tt> if no <tt>URN</tt> could be created
	 *
	 * @see com.limegroup.gnutella.Acceptor
	 */
	public static URN createSHA1UrnFromHttpRequest(final String requestLine) 
		throws IOException {
		if(!URN.isValidUrnHttpRequest(requestLine)) {
			throw new IOException("INVALID URN HTTP REQUEST");
		}
		String urnString = URN.extractUrnFromHttpRequest(requestLine);
		if(urnString == null) {
			throw new IOException("COULD NOT CONSTRUCT URN");
		}	   
		return createSHA1Urn(urnString);
	}
    
    /**
     * Creates a SHA1 URN from a byte[].
     */
    public static URN createSHA1UrnFromBytes(byte[] bytes) throws IOException {
        if(bytes == null || bytes.length != 20)
            throw new IOException("invalid bytes!");
        
        String hash = Base32.encode(bytes);
        return createSHA1UrnFromString("urn:sha1:" + hash);
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
		if(urnString == null) {
			throw new IOException("cannot accept null URN string");
		}
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
     * Constructs a new SHA1 URN from a bitprint URN
     * 
     * @param bitprintString
     *            the string for the bitprint
     * @return a new <tt>URN</tt> built from the specified string
     * @throws <tt>IOException</tt> if there is an error
     */
    private static URN createSHA1UrnFromBitprint(final String bitprintString)
        throws IOException {
        // extract the BASE32 encoded SHA1 from the bitprint
        int dotIdx = bitprintString.indexOf(DOT);
        if(dotIdx == -1)
            throw new IOException("invalid bitprint: " + bitprintString);

        String sha1 =
            bitprintString.substring(
                bitprintString.indexOf(':', 4) + 1, dotIdx);

        return createSHA1UrnFromString(
            UrnType.URN_NAMESPACE_ID + UrnType.SHA1_STRING + sha1);
    }
    
	/**
     * Gets the TTRoot from a bitprint string.
     */
    private static String getTTRootFromBitprint(final String bitprintString)
      throws IOException {
        int dotIdx = bitprintString.indexOf(DOT);
        if(dotIdx == -1 || dotIdx == bitprintString.length() - 1)
            throw new IOException("invalid bitprint: " + bitprintString);

        String tt = bitprintString.substring(dotIdx + 1);
        if(tt.length() != 39)
            throw new IOException("wrong length: " + tt.length());

        return tt;
    }
    
	/**
	 * Constructs a new URN based on the specified <tt>File</tt> instance.
	 * The constructor calculates the SHA1 value for the file, and is a
	 * costly operation as a result.
	 *
	 * @param file the <tt>File</tt> instance to construct the URN from
	 * @param urnType the type of URN to construct for the <tt>File</tt>
	 *  instance, such as SHA1_URN
	 */
	private URN(final String urnString, final UrnType urnType) {
        int lastColon = urnString.lastIndexOf(":");
        String nameSpace = urnString.substring(0,lastColon+1);
        String hash = urnString.substring(lastColon+1);
		this._urnString = nameSpace.toLowerCase(Locale.US) +
                                  hash.toUpperCase(Locale.US);
		this._urnType = urnType;
	}
    
    /**
     * Returns the bytes of this URN.
     * 
     * TODO: If the URN wasn't stored in Base32, this will be wrong.
     *       We deal only with SHA1 right now, which will be Base32.
     */
    public byte[] getBytes() {
        int lastColon = _urnString.lastIndexOf(":");
        String hash = _urnString.substring(lastColon+1);
        return Base32.decode(hash);        
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
        if (!(o instanceof URN)) return false;

        // Since hashCode is cached, this speeds comparison 
        // without affecting accuracy.
        if (this.hashCode() != o.hashCode()) {
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
	 * This.method checks whether or not the specified string fits the
	 * /uri-res/N2R?urn:sha1: format.  It does so by checking the start of the
	 * string as well as verifying the overall length.
	 *
	 * @param sha1String the string to check
	 * @return <tt>true</tt> if the string follows the proper format, otherwise
	 *  <tt>false</tt>
	 */
	private static boolean isValidUriResSHA1Format(final String sha1String) {
		String copy = sha1String.toLowerCase(Locale.US);		
		if(copy.startsWith("/uri-res/n2r?urn:sha1:")) {
			// just check the length
			return sha1String.length() == 54;
		} 
		return false;
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
	    return (URN.isValidLength(requestLine) &&
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
	private static final boolean isValidLength(final String requestLine) {
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
		if(firstSlash == -1 || firstSlash == requestLine.length()) {
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
	 * a URN, return the named resource," and N2X.
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
		String n2s = requestLine.substring(nIndex-1, nIndex+3);

		// we could add more protocols to this check
		if(!n2s.equalsIgnoreCase(HTTPConstants.NAME_TO_RESOURCE)
           && !n2s.equalsIgnoreCase(HTTPConstants.NAME_TO_THEX)) {
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
	private static String getTypeString(final String fullUrnString)
	  throws IOException {		
		// trims any leading whitespace from the urn string -- without 
		// whitespace the urn must start with 'urn:'
		String type = fullUrnString.trim();
		if(type.length() <= 4)
		    throw new IOException("no type string");

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
        
		MessageDigest md = new SHA1();
        byte[] buffer = new byte[65536];
        int read;
        IntWrapper progress = new IntWrapper(0);
        progressMap.put( file, progress );
        FileInputStream fis = null;        
        
        try {
		    fis = new FileInputStream(file);
            while ((read=fis.read(buffer))!=-1) {
                long start = System.currentTimeMillis();
                md.update(buffer,0,read);
                progress.addInt( read );
                if(SystemUtils.getIdleTime() < MIN_IDLE_TIME &&
		    SharingSettings.FRIENDLY_HASHING.getValue()) {
                    long end = System.currentTimeMillis();
                    long interval = end - start;
                    if(interval > 0)
                        Thread.sleep(interval * 3);
                    else
                        Thread.yield();
                }
            }
        } finally {		
            progressMap.remove(file);
            if(fis != null) {
                try {
                    fis.close();
                } catch(IOException ignored) {}
            }
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
		if(colon1Index == -1 || colon1Index+1 > urnString.length()) {
			return false;
		}

		int urnIndex1 = colon1Index-3;
		int urnIndex2 = colon1Index+1;

		if((urnIndex1 < 0) || (urnIndex2 < 0)) {
			return false;
		}

		// get the last colon -- this should separate the <NID>
		// from the <NIS>
		int colon2Index = urnString.indexOf(":", colon1Index+1);
		
		if(colon2Index == -1 || colon2Index+1 > urnString.length())
		    return false;
		
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
