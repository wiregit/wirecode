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
	public static final String URN = "urn:";

	/**
	 * The identifier for a SHA1 Namespace Identifier string.  This
	 * is "sha1" and should also be used in a case-insensitive manner.
	 */
	public static final String SHA1 = "sha1";

	/**
	 * Constant for the "GET" in a standard URN GET request.
	 */
	public static final String GET = "GET";
	
	/**
	 * Constant for the URN identifier constant combined with the
	 * SHA1 identifier constant.  This is used to identify SHA1 URNs.
	 */
	public static final String URN_SHA1 = URN+SHA1;

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
	 */
	public URN(File file) {
        try {
            // update modTime
            long modTime = file.lastModified();
            
            FileInputStream fis = new FileInputStream(file);   
            // we can only calculate SHA1 for now
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] buffer = new byte[16384];
            int read;
            while ((read=fis.read(buffer))!=-1) {
                md.update(buffer,0,read);
            }
            fis.close();
            byte[] sha1 = md.digest();
            //if(_urns==null) _urns = new HashSet();
            // preferred casing: lowercase "urn:sha1:", uppercase encoded value
            // note that all URNs are case-insensitive for the "urn:<type>:" part,
            // but some MAY be case-sensitive thereafter (SHA1/Base32 is case 
			// insensitive)
            //_urns.add("urn:sha1:"+Base32.encode(sha1));
			_urnString = URN_SHA1 + ":" + Base32.encode(sha1);
        } catch (IOException e) {
            // relatively harmless to not have URN
        } catch (NoSuchAlgorithmException e) {
            // relatively harmless to not have URN    
        }		
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
	public boolean equals(URN urn) {
		return urn.getURNString().equals(_urnString);
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
