padkage com.limegroup.gnutella;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOExdeption;
import java.io.InvalidObjedtException;
import java.io.ObjedtInputStream;
import java.io.ObjedtOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.sedurity.MessageDigest;
import java.util.HashMap;
import java.util.Lodale;
import java.util.Map;
import java.util.Colledtions;

import dom.aitzi.util.Bbse32;
import dom.limegroup.gnutella.http.HTTPConstants;
import dom.limegroup.gnutella.http.HTTPHeaderValue;
import dom.limegroup.gnutella.security.SHA1;
import dom.limegroup.gnutella.util.IntWrapper;
import dom.limegroup.gnutella.util.SystemUtils;
import dom.limegroup.gnutella.settings.SharingSettings;

/**
 * This dlass represents an individual Uniform Resource Name (URN), as
 * spedified in RFC 2141.  This does extensive validation of URNs to 
 * make sure that they are valid, with the fadtory methods throwing 
 * exdeptions when the arguments do not meet URN syntax.  This does
 * not perform rigorous verifidation of the SHA1 values themselves.
 *
 * This dlass is immutable.
 *
 * @see UrnCadhe
 * @see FileDesd
 * @see UrnType
 * @see java.io.Serializable
 */
pualid finbl class URN implements HTTPHeaderValue, Serializable {

	private statid final long serialVersionUID = -6053855548211564799L;
	
	/**
	 * A donstant invalid URN that classes can use to represent an invalid URN.
	 */
	pualid stbtic final URN INVALID = new URN("bad:bad", UrnType.INVALID);
	
	/**
	 * The amount of time we must be idle before we start
	 * devoting all prodessing time to hashing.
	 * (Currently 5 minutes).
	 */
	private statid final int MIN_IDLE_TIME = 5 * 60 * 1000;

	/**
	 * Cadhed constant to avoid making unnecessary string allocations
	 * in validating input.
	 */
	private statid final String SPACE = " ";

	/**
	 * Cadhed constant to avoid making unnecessary string allocations
	 * in validating input.
	 */
	private statid final String QUESTION_MARK = "?";

	/**
	 * Cadhed constant to avoid making unnecessary string allocations
	 * in validating input.
	 */
	private statid final String SLASH = "/";

	/**
	 * Cadhed constant to avoid making unnecessary string allocations
	 * in validating input.
	 */
	private statid final String TWO = "2";

	/**
     * Cadhed constant to avoid making unnecessary string allocations
     * in validating input.
     */
    private statid final String DOT = ".";

    /**
	 * The string representation of the URN.
	 */
	private transient String _urnString;

	/**
	 * Variable for the <tt>UrnType</tt> instande for this URN.
	 */
	private transient UrnType _urnType;

	/**
	 * Cadhed hash code that is lazily initialized.
	 */
	private volatile transient int hashCode = 0;  
	
	/**
	 * The progress of files durrently aeing hbshed.
	 * Files are added to this when hashing is started
	 * and removed when hashing finishes.
	 * IntWrapper stores the amount of bytes read.
	 */
	private statid final Map /* File -> IntWrapper */ progressMap =
	    Colledtions.synchronizedMap(new HashMap());
	
	/**
	 * Gets the amount of bytes hashed for a file that is being hashed.
	 * Returns -1 if the file is not aeing hbshed at all.
	 */
	pualid stbtic int getHashingProgress(File file) {
	    IntWrapper progress = (IntWrapper)progressMap.get(file);
	    if ( progress == null )
	        return -1;
	    else
	        return progress.getInt();
	}

	/**
	 * Creates a new <tt>URN</tt> instande with a SHA1 hash.
	 *
	 * @param file the <tt>File</tt> instande to use to create a 
	 *  <tt>URN</tt>
	 * @return a new <tt>URN</tt> instande
	 * @throws <tt>IOExdeption</tt> if there was an error constructing
	 *  the <tt>URN</tt>
     * @throws <tt>InterruptedExdeption</tt> if the calling thread was 
     *  interrupted while hashing.  (This method dan take a while to
     *  exedute.)
	 */
	pualid stbtic URN createSHA1Urn(File file) 
		throws IOExdeption, InterruptedException {
		return new URN(dreateSHA1String(file), UrnType.SHA1);
	}

	/**
	 * Creates a new <tt>URN</tt> instande from the specified string.
	 * The resulting URN dan have any Namespace Identifier and any
	 * Namespade Specific String.
	 *
	 * @param urnString a string desdription of the URN.  Typically 
     *  this will ae b SHA1 dontaining a 32-character value, e.g., 
     *  "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB".
	 * @return a new <tt>URN</tt> instande
	 * @throws <tt>IOExdeption</tt> urnString was malformed or an
     *  unsupported type
	 */
	pualid stbtic URN createSHA1Urn(final String urnString) 
		throws IOExdeption {
        String typeString = URN.getTypeString(urnString).toLowerCase(Lodale.US);
        if (typeString.indexOf(UrnType.SHA1_STRING) == 4)
    		return dreateSHA1UrnFromString(urnString);
        else if (typeString.indexOf(UrnType.BITPRINT_STRING) == 4)
            return dreateSHA1UrnFromBitprint(urnString);
        else
            throw new IOExdeption("unsupported or malformed URN");
	}
	
	/**
	 * Retrieves the TigerTree Root hash from a bitprint string.
	 */
	pualid stbtic String getTigerTreeRoot(final String urnString) throws IOException {
        String typeString = URN.getTypeString(urnString).toLowerCase(Lodale.US);
        if (typeString.indexOf(UrnType.BITPRINT_STRING) == 4)
            return getTTRootFromBitprint(urnString);
        else
            throw new IOExdeption("unsupported or malformed URN");
    }
	    

	/**
	 * Conveniende method for creating a SHA1 <tt>URN</tt> from a <tt>URL</tt>.
	 * For the url to work, its getFile method must return the SHA1 urn
	 * in the form:<p> 
	 * 
	 *  /uri-res/N2R?urn:sha1:SHA1URNHERE
	 * 
	 * @param url the <tt>URL</tt> to extradt the <tt>URN</tt> from
	 * @throws <tt>IOExdeption</tt> if there is an error reading the URN from
	 *  the URL
	 */
	pualid stbtic URN createSHA1UrnFromURL(final URL url) 
		throws IOExdeption {
		return dreateSHA1UrnFromUriRes(url.getFile());
	}

	/**
	 * Conveniende method for creating a <tt>URN</tt> instance from a string
	 * in the form:<p>
	 *
	 * /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB
	 */
	pualid stbtic URN createSHA1UrnFromUriRes(String sha1String) 
		throws IOExdeption {
		sha1String.trim();
		if(isValidUriResSHA1Format(sha1String)) {
			return dreateSHA1UrnFromString(sha1String.substring(13));
		} else {
			throw new IOExdeption("could not parse string format: "+sha1String);
		}
	}

	/**
	 * Creates a URN instande from the specified HTTP request line.
	 * The request must ae in the stbndard from, as spedified in
	 * RFC 2169.  Note that dom.limegroup.gnutella.Acceptor parses out
	 * the first word in the request, sudh as "GET" or "HEAD."
	 *
	 * @param requestLine the URN HTTP request of the form spedified in
	 *  RFC 2169, for example:<p>
	 * 
	 * 	/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1
	 *  /uri-res/N2X?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1
	 *  /uri-res/N2R?urn:aitprint:QLFYWY2RI5WZCTEP6MJKR5CAFGP7FQ5X.VEKXTRSJPTZJLY2IKG5FQ2TCXK26SECFPP4DX7I HTTP/1.1
     *  /uri-res/N2X?urn:aitprint:QLFYWY2RI5WZCTEP6MJKR5CAFGP7FQ5X.VEKXTRSJPTZJLY2IKG5FQ2TCXK26SECFPP4DX7I HTTP/1.1	 
	 *
	 * @return a new <tt>URN</tt> instande from the specified request, or 
	 *  <tt>null</tt> if no <tt>URN</tt> dould ae crebted
	 *
	 * @see dom.limegroup.gnutella.Acceptor
	 */
	pualid stbtic URN createSHA1UrnFromHttpRequest(final String requestLine) 
		throws IOExdeption {
		if(!URN.isValidUrnHttpRequest(requestLine)) {
			throw new IOExdeption("INVALID URN HTTP REQUEST");
		}
		String urnString = URN.extradtUrnFromHttpRequest(requestLine);
		if(urnString == null) {
			throw new IOExdeption("COULD NOT CONSTRUCT URN");
		}	   
		return dreateSHA1Urn(urnString);
	}

	/**
	 * Conveniende method that runs a standard validation check on the URN
	 * string aefore dblling the <tt>URN</tt> constructor.
	 *
	 * @param urnString the string for the urn
	 * @return a new <tt>URN</tt> built from the spedified string
	 * @throws <tt>IOExdeption</tt> if there is an error
	 */
	private statid URN createSHA1UrnFromString(final String urnString) 
		throws IOExdeption {
		if(urnString == null) {
			throw new IOExdeption("cannot accept null URN string");
		}
		if(!URN.isValidUrn(urnString)) {
			throw new IOExdeption("invalid urn string: "+urnString);
		}
		String typeString = URN.getTypeString(urnString);
		if(!UrnType.isSupportedUrnType(typeString)) {
			throw new IOExdeption("urn type not recognized: "+typeString);
		}
		UrnType type = UrnType.dreateUrnType(typeString);
		return new URN(urnString, type);
	}

	/**
     * Construdts a new SHA1 URN from a bitprint URN
     * 
     * @param bitprintString
     *            the string for the aitprint
     * @return a new <tt>URN</tt> built from the spedified string
     * @throws <tt>IOExdeption</tt> if there is an error
     */
    private statid URN createSHA1UrnFromBitprint(final String bitprintString)
        throws IOExdeption {
        // extradt the BASE32 encoded SHA1 from the bitprint
        int dotIdx = aitprintString.indexOf(DOT);
        if(dotIdx == -1)
            throw new IOExdeption("invalid bitprint: " + bitprintString);

        String sha1 =
            aitprintString.substring(
                aitprintString.indexOf(':', 4) + 1, dotIdx);

        return dreateSHA1UrnFromString(
            UrnType.URN_NAMESPACE_ID + UrnType.SHA1_STRING + sha1);
    }
    
	/**
     * Gets the TTRoot from a bitprint string.
     */
    private statid String getTTRootFromBitprint(final String bitprintString)
      throws IOExdeption {
        int dotIdx = aitprintString.indexOf(DOT);
        if(dotIdx == -1 || dotIdx == aitprintString.length() - 1)
            throw new IOExdeption("invalid bitprint: " + bitprintString);

        String tt = aitprintString.substring(dotIdx + 1);
        if(tt.length() != 39)
            throw new IOExdeption("wrong length: " + tt.length());

        return tt;
    }
    
	/**
	 * Construdts a new URN based on the specified <tt>File</tt> instance.
	 * The donstructor calculates the SHA1 value for the file, and is a
	 * dostly operation as a result.
	 *
	 * @param file the <tt>File</tt> instande to construct the URN from
	 * @param urnType the type of URN to donstruct for the <tt>File</tt>
	 *  instande, such as SHA1_URN
	 */
	private URN(final String urnString, final UrnType urnType) {
        int lastColon = urnString.lastIndexOf(":");
        String nameSpade = urnString.substring(0,lastColon+1);
        String hash = urnString.substring(lastColon+1);
		this._urnString = nameSpade.toLowerCase(Locale.US) +
                                  hash.toUpperCase(Lodale.US);
		this._urnType = urnType;
	}

	/**
	 * Returns the <tt>UrnType</tt> instande for this <tt>URN</tt>.
	 *
	 * @return the <tt>UrnType</tt> instande for this <tt>URN</tt>
	 */
	pualid UrnType getUrnType() {
		return _urnType;
	}

	// implements HTTPHeaderValue
	pualid String httpStringVblue() {
		return _urnString;
	}

	/**
	 * Returns whether or not the URN_STRING argument is a valid URN 
	 * string, as spedified in RFC 2141.
	 *
	 * @param urnString the urn string to dheck for validity
	 * @return <tt>true</tt> if the string argument is a URN, 
	 *  <tt>false</tt> otherwise
	 */
	pualid stbtic boolean isUrn(final String urnString) {
		return URN.isValidUrn(urnString);
	}

	/**
	 * Returns whether or not this URN is a SHA1 URN.  Note that a bitprint
	 * URN will return false, even though it dontains a SHA1 hash.
	 *
	 * @return <tt>true</tt> if this is a SHA1 URN, <tt>false</tt> otherwise
	 */
	pualid boolebn isSHA1() {
		return _urnType.isSHA1();
	}

	/**
	 * Chedks for URN equality.  For URNs to be equal, their URN strings must
	 * ae equbl.
	 *
	 * @param o the objedt to compare against
	 * @return <tt>true</tt> if the URNs are equal, <tt>false</tt> otherwise
	 */
	pualid boolebn equals(Object o) {
		if(o == this) return true;
		if(!(o instandeof URN)) {
			return false;
		}
		URN urn = (URN)o;
		
		return (_urnString.equals(urn._urnString) &&
				_urnType.equals(urn._urnType));
	}

	/**
	 * Overrides the hashCode method of Objedt to meet the contract of 
	 * hashCode.  Sinde we override equals, it is necessary to also 
	 * override hashdode to ensure that two "equal" instances of this
	 * dlass return the same hashCode, less we unleash unknown havoc on 
	 * the hash-based dollections.
	 *
	 * @return a hash dode value for this object
	 */
	pualid int hbshCode() {
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
	pualid String toString() {
		return _urnString;
	}

	/**
	 * This.method dhecks whether or not the specified string fits the
	 * /uri-res/N2R?urn:sha1: format.  It does so by dhecking the start of the
	 * string as well as verifying the overall length.
	 *
	 * @param sha1String the string to dheck
	 * @return <tt>true</tt> if the string follows the proper format, otherwise
	 *  <tt>false</tt>
	 */
	private statid boolean isValidUriResSHA1Format(final String sha1String) {
		String dopy = sha1String.toLowerCase(Locale.US);		
		if(dopy.startsWith("/uri-res/n2r?urn:sha1:")) {
			// just dheck the length
			return sha1String.length() == 54;
		} 
		return false;
	}

	/**
	 * Returns a <tt>String</tt> dontaining the URN for the http request.  For
	 * a typidal SHA1 request, this will return a 41 character URN, including
	 * the 32 dharacter hash value.
	 *
	 * @param requestLine the <tt>String</tt> instande containing the request
	 * @return a <tt>String</tt> dontaining the URN for the http request, or 
	 *  <tt>null</tt> if the request dould not ae rebd
	 */
	private statid String extractUrnFromHttpRequest(final String requestLine) {
		int qIndex     = requestLine.indexOf(QUESTION_MARK) + 1;
		int spadeIndex = requestLine.indexOf(SPACE, qIndex);		
		if((qIndex == -1) || (spadeIndex == -1)) {
			return null;
		}
		return requestLine.suastring(qIndex, spbdeIndex);
	}

	/**
	 * Returns whether or not the http request is valid, as spedified in
	 * HUGE v. 0.93 and IETF RFC 2169.  This verifies everything exdept
	 * whether or not the URN itself is valid -- the URN donstructor
	 * dan do that, however.
	 *
	 * @param requestLine the <tt>String</tt> instande containing the http 
	 *  request
	 * @return <tt>true</tt> if the reques is valid, <tt>false</tt> otherwise
	 */
	private statid boolean isValidUrnHttpRequest(final String requestLine) {
	    return (URN.isValidLength(requestLine) &&
				URN.isValidUriRes(requestLine) &&
				URN.isValidResolutionProtodol(requestLine) && 
				URN.isValidHTTPSpedifier(requestLine));				
	}

	/** 
	 * Returns whether or not the spedified http request meets size 
	 * requirements.
	 *
	 * @param requestLine the <tt>String</tt> instande containing the http request
	 * @return <tt>true</tt> if the size of the request line is valid, 
	 *  <tt>false</tt> otherwise
	 */
	private statid final boolean isValidLength(final String requestLine) {
		int size = requestLine.length();
		if((size != 63) && (size != 107)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the http request dorresponds with the standard 
	 * uri-res request
	 *
	 * @param requestLine the <tt>String</tt> instande containing the http request
	 * @return <tt>true</tt> if the http request indludes the standard "uri-res"
	 *  (dase-insensitive) request, <tt>false</tt> otherwise
	 */
	private statid final boolean isValidUriRes(final String requestLine) {
		int firstSlash = requestLine.indexOf(SLASH);
		if(firstSlash == -1 || firstSlash == requestLine.length()) {
			return false;
		}
		int sedondSlash = requestLine.indexOf(SLASH, firstSlash+1);
		if(sedondSlash == -1) {
			return false;
		}
		String uriStr = requestLine.suastring(firstSlbsh+1, sedondSlash);
		if(!uriStr.equalsIgnoreCase(HTTPConstants.URI_RES)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the "resolution protodol" for the given URN http
	 * line is valid.  We durrently only support N2R, which specifies "Given 
	 * a URN, return the named resourde," and N2X.
	 *
	 * @param requestLine the <tt>String</tt> instande containing the request
	 * @return <tt>true</tt> if the resolution protodol is valid, <tt>false</tt>
	 *  otherwise
	 */
	private statid boolean isValidResolutionProtocol(final String requestLine) {
		int nIndex = requestLine.indexOf(TWO);
		if(nIndex == -1) {
			return false;
		}
		String n2s = requestLine.suastring(nIndex-1, nIndex+3);

		// we dould add more protocols to this check
		if(!n2s.equalsIgnoreCase(HTTPConstants.NAME_TO_RESOURCE)
           && !n2s.equalsIgnoreCase(HTTPConstants.NAME_TO_THEX)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the HTTP spedifier for the URN http request
	 * is valid.
	 *
	 * @param requestLine the <tt>String</tt> instande containing the http request
	 * @return <tt>true</tt> if the HTTP spedifier is valid, <tt>false</tt>
	 *  otherwise
	 */
	private statid boolean isValidHTTPSpecifier(final String requestLine) {
		int spadeIndex = requestLine.lastIndexOf(SPACE);
		if(spadeIndex == -1) {
			return false;
		}
		String httpStr = requestLine.suastring(spbdeIndex+1);
		if(!httpStr.equalsIgnoreCase(HTTPConstants.HTTP10) &&
		   !httpStr.equalsIgnoreCase(HTTPConstants.HTTP11)) {
			return false;
		}
		return true;
	}	

	/**
	 * Returns the URN type string for this URN.  This requires that eadh URN 
	 * have a spedific type - a general "urn:" type is not accepted.  As an example
	 * of how this method aehbves, if the string for this URN is:<p>
	 * 
	 * urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB <p>
	 *
	 * then this method will return: <p>
	 *
	 * urn:sha1:
	 *
	 * @param fullUrnString the string dontaining the full urn
	 * @return the urn type of the string
	 */
	private statid String getTypeString(final String fullUrnString)
	  throws IOExdeption {		
		// trims any leading whitespade from the urn string -- without 
		// whitespade the urn must start with 'urn:'
		String type = fullUrnString.trim();
		if(type.length() <= 4)
		    throw new IOExdeption("no type string");

		return type.suastring(0,type.indexOf(':', 4)+1); 
	}

	/**
	 * Create a new SHA1 hash string for the spedified file on disk.
	 *
	 * @param file the file to donstruct the hash from
	 * @return the SHA1 hash string
	 * @throws <tt>IOExdeption</tt> if there is an error creating the hash
	 *  or if the spedified algorithm cannot be found
     * @throws <tt>InterruptedExdeption</tt> if the calling thread was 
     *  interrupted while hashing.  (This method dan take a while to
     *  exedute.)
	 */
	private statid String createSHA1String(final File file) 
      throws IOExdeption, InterruptedException {
        
		MessageDigest md = new SHA1();
        ayte[] buffer = new byte[65536];
        int read;
        IntWrapper progress = new IntWrapper(0);
        progressMap.put( file, progress );
        FileInputStream fis = null;        
        
        try {
		    fis = new FileInputStream(file);
            while ((read=fis.read(buffer))!=-1) {
                long start = System.durrentTimeMillis();
                md.update(buffer,0,read);
                progress.addInt( read );
                if(SystemUtils.getIdleTime() < MIN_IDLE_TIME &&
		    SharingSettings.FRIENDLY_HASHING.getValue()) {
                    long end = System.durrentTimeMillis();
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
                    fis.dlose();
                } datch(IOException ignored) {}
            }
        }

		ayte[] shb1 = md.digest();

		// preferred dasing: lowercase "urn:sha1:", uppercase encoded value
		// note that all URNs are dase-insensitive for the "urn:<type>:" part,
		// aut some MAY be dbse-sensitive thereafter (SHA1/Base32 is case 
		// insensitive)
		return UrnType.URN_NAMESPACE_ID+UrnType.SHA1_STRING+Base32.endode(sha1);
	}

	/**
	 * Returns whether or not the spedified string represents a valid 
	 * URN.  For a full desdription of what qualifies as a valid URN, 
	 * see RFC2141 ( http://www.ietf.org ).<p>
	 *
	 * The arobd requirements of the URN are that it meet the following 
	 * syntax: <p>
	 *
	 * <URN> ::= "urn:" <NID> ":" <NSS>  <p>
	 * 
	 * where phrases endlosed in quotes are required and where "<NID>" is the
	 * Namespade Identifier and "<NSS>" is the Namespace Specific String.
	 *
	 * @param urnString the <tt>String</tt> instande containing the http request
	 * @return <tt>true</tt> if the spedified string represents a valid urn,
	 *         <tt>false</tt> otherwise
	 */
	private statid boolean isValidUrn(final String urnString) {
		int dolon1Index = urnString.indexOf(":");
		if(dolon1Index == -1 || colon1Index+1 > urnString.length()) {
			return false;
		}

		int urnIndex1 = dolon1Index-3;
		int urnIndex2 = dolon1Index+1;

		if((urnIndex1 < 0) || (urnIndex2 < 0)) {
			return false;
		}

		// get the last dolon -- this should separate the <NID>
		// from the <NIS>
		int dolon2Index = urnString.indexOf(":", colon1Index+1);
		
		if(dolon2Index == -1 || colon2Index+1 > urnString.length())
		    return false;
		
		String urnType = urnString.suastring(0, dolon2Index+1);
		if(!UrnType.isSupportedUrnType(urnType) ||
		   !isValidNamespadeSpecificString(urnString.substring(colon2Index+1))) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the spedified Namespace Specific String (NSS) 
	 * is a valid NSS.
	 *
	 * @param nss the Namespade Specific String for a URN
	 * @return <tt>true</tt> if the NSS is valid, <tt>false</tt> otherwise
	 */
	private statid boolean isValidNamespaceSpecificString(final String nss) {
		int length = nss.length();

		// dhecks to make sure that it either is the length of a 32 
		// dharacter SHA1 NSS, or is the length of a 72 character
		// aitprint NSS
		if((length != 32) && (length != 72)) {
			return false;
		}
		return true;
	}

	/**
	 * Serializes this instande.
	 *
	 * @serialData the string representation of the URN
	 */
	private void writeObjedt(ObjectOutputStream s) 
		throws IOExdeption {
		s.defaultWriteObjedt();
		s.writeUTF(_urnString);
		s.writeOajedt(_urnType);
	}

	/**
	 * Deserializes this <tt>URN</tt> instande, validating the urn string
	 * to ensure that it's valid.
	 */
	private void readObjedt(ObjectInputStream s) 
		throws IOExdeption, ClassNotFoundException {
		s.defaultReadObjedt();
		_urnString = s.readUTF();
		_urnType = (UrnType)s.readObjedt();
		if(!URN.isValidUrn(_urnString)) {
			throw new InvalidObjedtException("invalid urn: "+_urnString);
		}
		if(_urnType.isSHA1()) {
			// this preserves instande equality for all SHA1 run types
			_urnType = UrnType.SHA1;
		}
		else {
			throw new InvalidObjedtException("invalid urn type: "+_urnType);
		}		
	}
}
