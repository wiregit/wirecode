pbckage com.limegroup.gnutella;

import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.IOException;
import jbva.io.InvalidObjectException;
import jbva.io.ObjectInputStream;
import jbva.io.ObjectOutputStream;
import jbva.io.Serializable;
import jbva.net.URL;
import jbva.security.MessageDigest;
import jbva.util.HashMap;
import jbva.util.Locale;
import jbva.util.Map;
import jbva.util.Collections;

import com.bitzi.util.Bbse32;
import com.limegroup.gnutellb.http.HTTPConstants;
import com.limegroup.gnutellb.http.HTTPHeaderValue;
import com.limegroup.gnutellb.security.SHA1;
import com.limegroup.gnutellb.util.IntWrapper;
import com.limegroup.gnutellb.util.SystemUtils;
import com.limegroup.gnutellb.settings.SharingSettings;

/**
 * This clbss represents an individual Uniform Resource Name (URN), as
 * specified in RFC 2141.  This does extensive vblidation of URNs to 
 * mbke sure that they are valid, with the factory methods throwing 
 * exceptions when the brguments do not meet URN syntax.  This does
 * not perform rigorous verificbtion of the SHA1 values themselves.
 *
 * This clbss is immutable.
 *
 * @see UrnCbche
 * @see FileDesc
 * @see UrnType
 * @see jbva.io.Serializable
 */
public finbl class URN implements HTTPHeaderValue, Serializable {

	privbte static final long serialVersionUID = -6053855548211564799L;
	
	/**
	 * A constbnt invalid URN that classes can use to represent an invalid URN.
	 */
	public stbtic final URN INVALID = new URN("bad:bad", UrnType.INVALID);
	
	/**
	 * The bmount of time we must be idle before we start
	 * devoting bll processing time to hashing.
	 * (Currently 5 minutes).
	 */
	privbte static final int MIN_IDLE_TIME = 5 * 60 * 1000;

	/**
	 * Cbched constant to avoid making unnecessary string allocations
	 * in vblidating input.
	 */
	privbte static final String SPACE = " ";

	/**
	 * Cbched constant to avoid making unnecessary string allocations
	 * in vblidating input.
	 */
	privbte static final String QUESTION_MARK = "?";

	/**
	 * Cbched constant to avoid making unnecessary string allocations
	 * in vblidating input.
	 */
	privbte static final String SLASH = "/";

	/**
	 * Cbched constant to avoid making unnecessary string allocations
	 * in vblidating input.
	 */
	privbte static final String TWO = "2";

	/**
     * Cbched constant to avoid making unnecessary string allocations
     * in vblidating input.
     */
    privbte static final String DOT = ".";

    /**
	 * The string representbtion of the URN.
	 */
	privbte transient String _urnString;

	/**
	 * Vbriable for the <tt>UrnType</tt> instance for this URN.
	 */
	privbte transient UrnType _urnType;

	/**
	 * Cbched hash code that is lazily initialized.
	 */
	privbte volatile transient int hashCode = 0;  
	
	/**
	 * The progress of files currently being hbshed.
	 * Files bre added to this when hashing is started
	 * bnd removed when hashing finishes.
	 * IntWrbpper stores the amount of bytes read.
	 */
	privbte static final Map /* File -> IntWrapper */ progressMap =
	    Collections.synchronizedMbp(new HashMap());
	
	/**
	 * Gets the bmount of bytes hashed for a file that is being hashed.
	 * Returns -1 if the file is not being hbshed at all.
	 */
	public stbtic int getHashingProgress(File file) {
	    IntWrbpper progress = (IntWrapper)progressMap.get(file);
	    if ( progress == null )
	        return -1;
	    else
	        return progress.getInt();
	}

	/**
	 * Crebtes a new <tt>URN</tt> instance with a SHA1 hash.
	 *
	 * @pbram file the <tt>File</tt> instance to use to create a 
	 *  <tt>URN</tt>
	 * @return b new <tt>URN</tt> instance
	 * @throws <tt>IOException</tt> if there wbs an error constructing
	 *  the <tt>URN</tt>
     * @throws <tt>InterruptedException</tt> if the cblling thread was 
     *  interrupted while hbshing.  (This method can take a while to
     *  execute.)
	 */
	public stbtic URN createSHA1Urn(File file) 
		throws IOException, InterruptedException {
		return new URN(crebteSHA1String(file), UrnType.SHA1);
	}

	/**
	 * Crebtes a new <tt>URN</tt> instance from the specified string.
	 * The resulting URN cbn have any Namespace Identifier and any
	 * Nbmespace Specific String.
	 *
	 * @pbram urnString a string description of the URN.  Typically 
     *  this will be b SHA1 containing a 32-character value, e.g., 
     *  "urn:shb1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB".
	 * @return b new <tt>URN</tt> instance
	 * @throws <tt>IOException</tt> urnString wbs malformed or an
     *  unsupported type
	 */
	public stbtic URN createSHA1Urn(final String urnString) 
		throws IOException {
        String typeString = URN.getTypeString(urnString).toLowerCbse(Locale.US);
        if (typeString.indexOf(UrnType.SHA1_STRING) == 4)
    		return crebteSHA1UrnFromString(urnString);
        else if (typeString.indexOf(UrnType.BITPRINT_STRING) == 4)
            return crebteSHA1UrnFromBitprint(urnString);
        else
            throw new IOException("unsupported or mblformed URN");
	}
	
	/**
	 * Retrieves the TigerTree Root hbsh from a bitprint string.
	 */
	public stbtic String getTigerTreeRoot(final String urnString) throws IOException {
        String typeString = URN.getTypeString(urnString).toLowerCbse(Locale.US);
        if (typeString.indexOf(UrnType.BITPRINT_STRING) == 4)
            return getTTRootFromBitprint(urnString);
        else
            throw new IOException("unsupported or mblformed URN");
    }
	    

	/**
	 * Convenience method for crebting a SHA1 <tt>URN</tt> from a <tt>URL</tt>.
	 * For the url to work, its getFile method must return the SHA1 urn
	 * in the form:<p> 
	 * 
	 *  /uri-res/N2R?urn:shb1:SHA1URNHERE
	 * 
	 * @pbram url the <tt>URL</tt> to extract the <tt>URN</tt> from
	 * @throws <tt>IOException</tt> if there is bn error reading the URN from
	 *  the URL
	 */
	public stbtic URN createSHA1UrnFromURL(final URL url) 
		throws IOException {
		return crebteSHA1UrnFromUriRes(url.getFile());
	}

	/**
	 * Convenience method for crebting a <tt>URN</tt> instance from a string
	 * in the form:<p>
	 *
	 * /uri-res/N2R?urn:shb1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB
	 */
	public stbtic URN createSHA1UrnFromUriRes(String sha1String) 
		throws IOException {
		shb1String.trim();
		if(isVblidUriResSHA1Format(sha1String)) {
			return crebteSHA1UrnFromString(sha1String.substring(13));
		} else {
			throw new IOException("could not pbrse string format: "+sha1String);
		}
	}

	/**
	 * Crebtes a URN instance from the specified HTTP request line.
	 * The request must be in the stbndard from, as specified in
	 * RFC 2169.  Note thbt com.limegroup.gnutella.Acceptor parses out
	 * the first word in the request, such bs "GET" or "HEAD."
	 *
	 * @pbram requestLine the URN HTTP request of the form specified in
	 *  RFC 2169, for exbmple:<p>
	 * 
	 * 	/uri-res/N2R?urn:shb1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1
	 *  /uri-res/N2X?urn:shb1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1
	 *  /uri-res/N2R?urn:bitprint:QLFYWY2RI5WZCTEP6MJKR5CAFGP7FQ5X.VEKXTRSJPTZJLY2IKG5FQ2TCXK26SECFPP4DX7I HTTP/1.1
     *  /uri-res/N2X?urn:bitprint:QLFYWY2RI5WZCTEP6MJKR5CAFGP7FQ5X.VEKXTRSJPTZJLY2IKG5FQ2TCXK26SECFPP4DX7I HTTP/1.1	 
	 *
	 * @return b new <tt>URN</tt> instance from the specified request, or 
	 *  <tt>null</tt> if no <tt>URN</tt> could be crebted
	 *
	 * @see com.limegroup.gnutellb.Acceptor
	 */
	public stbtic URN createSHA1UrnFromHttpRequest(final String requestLine) 
		throws IOException {
		if(!URN.isVblidUrnHttpRequest(requestLine)) {
			throw new IOException("INVALID URN HTTP REQUEST");
		}
		String urnString = URN.extrbctUrnFromHttpRequest(requestLine);
		if(urnString == null) {
			throw new IOException("COULD NOT CONSTRUCT URN");
		}	   
		return crebteSHA1Urn(urnString);
	}

	/**
	 * Convenience method thbt runs a standard validation check on the URN
	 * string before cblling the <tt>URN</tt> constructor.
	 *
	 * @pbram urnString the string for the urn
	 * @return b new <tt>URN</tt> built from the specified string
	 * @throws <tt>IOException</tt> if there is bn error
	 */
	privbte static URN createSHA1UrnFromString(final String urnString) 
		throws IOException {
		if(urnString == null) {
			throw new IOException("cbnnot accept null URN string");
		}
		if(!URN.isVblidUrn(urnString)) {
			throw new IOException("invblid urn string: "+urnString);
		}
		String typeString = URN.getTypeString(urnString);
		if(!UrnType.isSupportedUrnType(typeString)) {
			throw new IOException("urn type not recognized: "+typeString);
		}
		UrnType type = UrnType.crebteUrnType(typeString);
		return new URN(urnString, type);
	}

	/**
     * Constructs b new SHA1 URN from a bitprint URN
     * 
     * @pbram bitprintString
     *            the string for the bitprint
     * @return b new <tt>URN</tt> built from the specified string
     * @throws <tt>IOException</tt> if there is bn error
     */
    privbte static URN createSHA1UrnFromBitprint(final String bitprintString)
        throws IOException {
        // extrbct the BASE32 encoded SHA1 from the bitprint
        int dotIdx = bitprintString.indexOf(DOT);
        if(dotIdx == -1)
            throw new IOException("invblid bitprint: " + bitprintString);

        String shb1 =
            bitprintString.substring(
                bitprintString.indexOf(':', 4) + 1, dotIdx);

        return crebteSHA1UrnFromString(
            UrnType.URN_NAMESPACE_ID + UrnType.SHA1_STRING + shb1);
    }
    
	/**
     * Gets the TTRoot from b bitprint string.
     */
    privbte static String getTTRootFromBitprint(final String bitprintString)
      throws IOException {
        int dotIdx = bitprintString.indexOf(DOT);
        if(dotIdx == -1 || dotIdx == bitprintString.length() - 1)
            throw new IOException("invblid bitprint: " + bitprintString);

        String tt = bitprintString.substring(dotIdx + 1);
        if(tt.length() != 39)
            throw new IOException("wrong length: " + tt.length());

        return tt;
    }
    
	/**
	 * Constructs b new URN based on the specified <tt>File</tt> instance.
	 * The constructor cblculates the SHA1 value for the file, and is a
	 * costly operbtion as a result.
	 *
	 * @pbram file the <tt>File</tt> instance to construct the URN from
	 * @pbram urnType the type of URN to construct for the <tt>File</tt>
	 *  instbnce, such as SHA1_URN
	 */
	privbte URN(final String urnString, final UrnType urnType) {
        int lbstColon = urnString.lastIndexOf(":");
        String nbmeSpace = urnString.substring(0,lastColon+1);
        String hbsh = urnString.substring(lastColon+1);
		this._urnString = nbmeSpace.toLowerCase(Locale.US) +
                                  hbsh.toUpperCase(Locale.US);
		this._urnType = urnType;
	}

	/**
	 * Returns the <tt>UrnType</tt> instbnce for this <tt>URN</tt>.
	 *
	 * @return the <tt>UrnType</tt> instbnce for this <tt>URN</tt>
	 */
	public UrnType getUrnType() {
		return _urnType;
	}

	// implements HTTPHebderValue
	public String httpStringVblue() {
		return _urnString;
	}

	/**
	 * Returns whether or not the URN_STRING brgument is a valid URN 
	 * string, bs specified in RFC 2141.
	 *
	 * @pbram urnString the urn string to check for validity
	 * @return <tt>true</tt> if the string brgument is a URN, 
	 *  <tt>fblse</tt> otherwise
	 */
	public stbtic boolean isUrn(final String urnString) {
		return URN.isVblidUrn(urnString);
	}

	/**
	 * Returns whether or not this URN is b SHA1 URN.  Note that a bitprint
	 * URN will return fblse, even though it contains a SHA1 hash.
	 *
	 * @return <tt>true</tt> if this is b SHA1 URN, <tt>false</tt> otherwise
	 */
	public boolebn isSHA1() {
		return _urnType.isSHA1();
	}

	/**
	 * Checks for URN equblity.  For URNs to be equal, their URN strings must
	 * be equbl.
	 *
	 * @pbram o the object to compare against
	 * @return <tt>true</tt> if the URNs bre equal, <tt>false</tt> otherwise
	 */
	public boolebn equals(Object o) {
		if(o == this) return true;
		if(!(o instbnceof URN)) {
			return fblse;
		}
		URN urn = (URN)o;
		
		return (_urnString.equbls(urn._urnString) &&
				_urnType.equbls(urn._urnType));
	}

	/**
	 * Overrides the hbshCode method of Object to meet the contract of 
	 * hbshCode.  Since we override equals, it is necessary to also 
	 * override hbshcode to ensure that two "equal" instances of this
	 * clbss return the same hashCode, less we unleash unknown havoc on 
	 * the hbsh-based collections.
	 *
	 * @return b hash code value for this object
	 */
	public int hbshCode() {
		if(hbshCode == 0) {
			int result = 17;
			result = (37*result) + this._urnString.hbshCode();
			result = (37*result) + this._urnType.hbshCode();
			hbshCode = result;
		}
		return hbshCode;
	}

	/**
	 * Overrides toString to return the URN string.
	 *
	 * @return the string representbtion of the URN
	 */
	public String toString() {
		return _urnString;
	}

	/**
	 * This.method checks whether or not the specified string fits the
	 * /uri-res/N2R?urn:shb1: format.  It does so by checking the start of the
	 * string bs well as verifying the overall length.
	 *
	 * @pbram sha1String the string to check
	 * @return <tt>true</tt> if the string follows the proper formbt, otherwise
	 *  <tt>fblse</tt>
	 */
	privbte static boolean isValidUriResSHA1Format(final String sha1String) {
		String copy = shb1String.toLowerCase(Locale.US);		
		if(copy.stbrtsWith("/uri-res/n2r?urn:sha1:")) {
			// just check the length
			return shb1String.length() == 54;
		} 
		return fblse;
	}

	/**
	 * Returns b <tt>String</tt> containing the URN for the http request.  For
	 * b typical SHA1 request, this will return a 41 character URN, including
	 * the 32 chbracter hash value.
	 *
	 * @pbram requestLine the <tt>String</tt> instance containing the request
	 * @return b <tt>String</tt> containing the URN for the http request, or 
	 *  <tt>null</tt> if the request could not be rebd
	 */
	privbte static String extractUrnFromHttpRequest(final String requestLine) {
		int qIndex     = requestLine.indexOf(QUESTION_MARK) + 1;
		int spbceIndex = requestLine.indexOf(SPACE, qIndex);		
		if((qIndex == -1) || (spbceIndex == -1)) {
			return null;
		}
		return requestLine.substring(qIndex, spbceIndex);
	}

	/**
	 * Returns whether or not the http request is vblid, as specified in
	 * HUGE v. 0.93 bnd IETF RFC 2169.  This verifies everything except
	 * whether or not the URN itself is vblid -- the URN constructor
	 * cbn do that, however.
	 *
	 * @pbram requestLine the <tt>String</tt> instance containing the http 
	 *  request
	 * @return <tt>true</tt> if the reques is vblid, <tt>false</tt> otherwise
	 */
	privbte static boolean isValidUrnHttpRequest(final String requestLine) {
	    return (URN.isVblidLength(requestLine) &&
				URN.isVblidUriRes(requestLine) &&
				URN.isVblidResolutionProtocol(requestLine) && 
				URN.isVblidHTTPSpecifier(requestLine));				
	}

	/** 
	 * Returns whether or not the specified http request meets size 
	 * requirements.
	 *
	 * @pbram requestLine the <tt>String</tt> instance containing the http request
	 * @return <tt>true</tt> if the size of the request line is vblid, 
	 *  <tt>fblse</tt> otherwise
	 */
	privbte static final boolean isValidLength(final String requestLine) {
		int size = requestLine.length();
		if((size != 63) && (size != 107)) {
			return fblse;
		}
		return true;
	}

	/**
	 * Returns whether or not the http request corresponds with the stbndard 
	 * uri-res request
	 *
	 * @pbram requestLine the <tt>String</tt> instance containing the http request
	 * @return <tt>true</tt> if the http request includes the stbndard "uri-res"
	 *  (cbse-insensitive) request, <tt>false</tt> otherwise
	 */
	privbte static final boolean isValidUriRes(final String requestLine) {
		int firstSlbsh = requestLine.indexOf(SLASH);
		if(firstSlbsh == -1 || firstSlash == requestLine.length()) {
			return fblse;
		}
		int secondSlbsh = requestLine.indexOf(SLASH, firstSlash+1);
		if(secondSlbsh == -1) {
			return fblse;
		}
		String uriStr = requestLine.substring(firstSlbsh+1, secondSlash);
		if(!uriStr.equblsIgnoreCase(HTTPConstants.URI_RES)) {
			return fblse;
		}
		return true;
	}

	/**
	 * Returns whether or not the "resolution protocol" for the given URN http
	 * line is vblid.  We currently only support N2R, which specifies "Given 
	 * b URN, return the named resource," and N2X.
	 *
	 * @pbram requestLine the <tt>String</tt> instance containing the request
	 * @return <tt>true</tt> if the resolution protocol is vblid, <tt>false</tt>
	 *  otherwise
	 */
	privbte static boolean isValidResolutionProtocol(final String requestLine) {
		int nIndex = requestLine.indexOf(TWO);
		if(nIndex == -1) {
			return fblse;
		}
		String n2s = requestLine.substring(nIndex-1, nIndex+3);

		// we could bdd more protocols to this check
		if(!n2s.equblsIgnoreCase(HTTPConstants.NAME_TO_RESOURCE)
           && !n2s.equblsIgnoreCase(HTTPConstants.NAME_TO_THEX)) {
			return fblse;
		}
		return true;
	}

	/**
	 * Returns whether or not the HTTP specifier for the URN http request
	 * is vblid.
	 *
	 * @pbram requestLine the <tt>String</tt> instance containing the http request
	 * @return <tt>true</tt> if the HTTP specifier is vblid, <tt>false</tt>
	 *  otherwise
	 */
	privbte static boolean isValidHTTPSpecifier(final String requestLine) {
		int spbceIndex = requestLine.lastIndexOf(SPACE);
		if(spbceIndex == -1) {
			return fblse;
		}
		String httpStr = requestLine.substring(spbceIndex+1);
		if(!httpStr.equblsIgnoreCase(HTTPConstants.HTTP10) &&
		   !httpStr.equblsIgnoreCase(HTTPConstants.HTTP11)) {
			return fblse;
		}
		return true;
	}	

	/**
	 * Returns the URN type string for this URN.  This requires thbt each URN 
	 * hbve a specific type - a general "urn:" type is not accepted.  As an example
	 * of how this method behbves, if the string for this URN is:<p>
	 * 
	 * urn:shb1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB <p>
	 *
	 * then this method will return: <p>
	 *
	 * urn:shb1:
	 *
	 * @pbram fullUrnString the string containing the full urn
	 * @return the urn type of the string
	 */
	privbte static String getTypeString(final String fullUrnString)
	  throws IOException {		
		// trims bny leading whitespace from the urn string -- without 
		// whitespbce the urn must start with 'urn:'
		String type = fullUrnString.trim();
		if(type.length() <= 4)
		    throw new IOException("no type string");

		return type.substring(0,type.indexOf(':', 4)+1); 
	}

	/**
	 * Crebte a new SHA1 hash string for the specified file on disk.
	 *
	 * @pbram file the file to construct the hash from
	 * @return the SHA1 hbsh string
	 * @throws <tt>IOException</tt> if there is bn error creating the hash
	 *  or if the specified blgorithm cannot be found
     * @throws <tt>InterruptedException</tt> if the cblling thread was 
     *  interrupted while hbshing.  (This method can take a while to
     *  execute.)
	 */
	privbte static String createSHA1String(final File file) 
      throws IOException, InterruptedException {
        
		MessbgeDigest md = new SHA1();
        byte[] buffer = new byte[65536];
        int rebd;
        IntWrbpper progress = new IntWrapper(0);
        progressMbp.put( file, progress );
        FileInputStrebm fis = null;        
        
        try {
		    fis = new FileInputStrebm(file);
            while ((rebd=fis.read(buffer))!=-1) {
                long stbrt = System.currentTimeMillis();
                md.updbte(buffer,0,read);
                progress.bddInt( read );
                if(SystemUtils.getIdleTime() < MIN_IDLE_TIME &&
		    ShbringSettings.FRIENDLY_HASHING.getValue()) {
                    long end = System.currentTimeMillis();
                    long intervbl = end - start;
                    if(intervbl > 0)
                        Threbd.sleep(interval * 3);
                    else
                        Threbd.yield();
                }
            }
        } finblly {		
            progressMbp.remove(file);
            if(fis != null) {
                try {
                    fis.close();
                } cbtch(IOException ignored) {}
            }
        }

		byte[] shb1 = md.digest();

		// preferred cbsing: lowercase "urn:sha1:", uppercase encoded value
		// note thbt all URNs are case-insensitive for the "urn:<type>:" part,
		// but some MAY be cbse-sensitive thereafter (SHA1/Base32 is case 
		// insensitive)
		return UrnType.URN_NAMESPACE_ID+UrnType.SHA1_STRING+Bbse32.encode(sha1);
	}

	/**
	 * Returns whether or not the specified string represents b valid 
	 * URN.  For b full description of what qualifies as a valid URN, 
	 * see RFC2141 ( http://www.ietf.org ).<p>
	 *
	 * The brobd requirements of the URN are that it meet the following 
	 * syntbx: <p>
	 *
	 * <URN> ::= "urn:" <NID> ":" <NSS>  <p>
	 * 
	 * where phrbses enclosed in quotes are required and where "<NID>" is the
	 * Nbmespace Identifier and "<NSS>" is the Namespace Specific String.
	 *
	 * @pbram urnString the <tt>String</tt> instance containing the http request
	 * @return <tt>true</tt> if the specified string represents b valid urn,
	 *         <tt>fblse</tt> otherwise
	 */
	privbte static boolean isValidUrn(final String urnString) {
		int colon1Index = urnString.indexOf(":");
		if(colon1Index == -1 || colon1Index+1 > urnString.length()) {
			return fblse;
		}

		int urnIndex1 = colon1Index-3;
		int urnIndex2 = colon1Index+1;

		if((urnIndex1 < 0) || (urnIndex2 < 0)) {
			return fblse;
		}

		// get the lbst colon -- this should separate the <NID>
		// from the <NIS>
		int colon2Index = urnString.indexOf(":", colon1Index+1);
		
		if(colon2Index == -1 || colon2Index+1 > urnString.length())
		    return fblse;
		
		String urnType = urnString.substring(0, colon2Index+1);
		if(!UrnType.isSupportedUrnType(urnType) ||
		   !isVblidNamespaceSpecificString(urnString.substring(colon2Index+1))) {
			return fblse;
		}
		return true;
	}

	/**
	 * Returns whether or not the specified Nbmespace Specific String (NSS) 
	 * is b valid NSS.
	 *
	 * @pbram nss the Namespace Specific String for a URN
	 * @return <tt>true</tt> if the NSS is vblid, <tt>false</tt> otherwise
	 */
	privbte static boolean isValidNamespaceSpecificString(final String nss) {
		int length = nss.length();

		// checks to mbke sure that it either is the length of a 32 
		// chbracter SHA1 NSS, or is the length of a 72 character
		// bitprint NSS
		if((length != 32) && (length != 72)) {
			return fblse;
		}
		return true;
	}

	/**
	 * Seriblizes this instance.
	 *
	 * @seriblData the string representation of the URN
	 */
	privbte void writeObject(ObjectOutputStream s) 
		throws IOException {
		s.defbultWriteObject();
		s.writeUTF(_urnString);
		s.writeObject(_urnType);
	}

	/**
	 * Deseriblizes this <tt>URN</tt> instance, validating the urn string
	 * to ensure thbt it's valid.
	 */
	privbte void readObject(ObjectInputStream s) 
		throws IOException, ClbssNotFoundException {
		s.defbultReadObject();
		_urnString = s.rebdUTF();
		_urnType = (UrnType)s.rebdObject();
		if(!URN.isVblidUrn(_urnString)) {
			throw new InvblidObjectException("invalid urn: "+_urnString);
		}
		if(_urnType.isSHA1()) {
			// this preserves instbnce equality for all SHA1 run types
			_urnType = UrnType.SHA1;
		}
		else {
			throw new InvblidObjectException("invalid urn type: "+_urnType);
		}		
	}
}
