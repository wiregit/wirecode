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
public final class URN {

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
	 * @param URN_TYPE the type of URN to construct for the <tt>File</tt>
	 *  instance, such as SHA1_URN
	 * @throws <tt>IllegalArgumentException</tt> if the URN type specified
	 *  is invalid
	 */
	public URN(final File FILE, final int URN_TYPE) throws IOException {
		switch(URN_TYPE) {
		case SHA1_URN:
			this.URN_STRING = URN.createSHA1String(FILE);
			break;
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
		this.URN_STRING = URN_STRING;
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
		if(URN_STRING.equals(URN.URN_NS_ID) || 
		   URN_STRING.equals(URN.URN_SHA1 + ":")) {
			return true;
		}
		return false;
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

		int urnIndex1 = colon1Index-3;
		int urnIndex2 = colon1Index+1;

		if((urnIndex1 < 0) || (urnIndex2 < 0)) {
			return false;
		}

		// get the "urn:" substring so we can make sure it's there,
		// ignoring case
		String urnStr = URN_STRING.substring(0, colon1Index+1);

		// get the last colon -- this should separate the <NID>
		// from the <NIS>
		int colon2Index = URN_STRING.indexOf(":", colon1Index+1);
		
		if((colon2Index == -1) || 
		   !urnStr.equalsIgnoreCase(URN.URN_NS_ID) ||
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
		return URN_STRING;
	}

	/**
	 * Returns whether or not this URN is a SHA1 URN.  Note that a bitprint
	 * URN will return false, even though it contains a SHA1 hash.
	 *
	 * @return <tt>true</tt> if this is a SHA1 URN, <tt>false</tt> otherwise
	 */
	public boolean isSHA1() {
		return URN_STRING.startsWith(URN_SHA1);
	}

	/**
	 * Checks for URN equality.  For URNs to be equal, their URN strings must
	 * be equal.
	 *
	 * @param urn the URN to compare against
	 * @return <tt>true</tt> if the URNs are equal, <tt>false</tt> otherwise
	 */
	public boolean equals(Object urn) {
		if(urn == this) return true;
		if(!(urn instanceof URN)) {
			return false;
		}
		return ((URN)urn).getURNString().equals(URN_STRING);
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

	
	/*
	public static void main(String[] args) {
		String [] validURNS = {
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "UrN:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sHa1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:bitprint:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
			             "PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567",
		    "urn:bitprint:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
			             "PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567"
		};

		String [] invalidURNS = {
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFBC",
		    "urn:sh1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "ur:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "rn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urnsha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sha1PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    " urn:sHa1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn::sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn: sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sha1 :PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sha1 :PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sha1: PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWU GYQYPFB",
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWU GYQYPFB ",
		    "urn:bitprint:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB.."+
			             "PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567",
		    "urn:bitprint:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB. "+
			             "PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567"
		};
		//UploadManager um = new UploadManager();
		boolean encounteredFailure = false;
		System.out.println("TESTING VALID URNS TO MAKE SURE THEY PASS TESTS..."); 
		for(int i=0; i<validURNS.length; i++) {
			try {
				URN urn = new URN(validURNS[i]);
			} catch(IOException e) {
				if(!encounteredFailure) {
					System.out.println(  ); 
					System.out.println("VALID URN TEST FAILED");
				}
				encounteredFailure = true;
				System.out.println(); 
				System.out.println("FAILED ON URN: ");
				System.out.print(validURNS[i]); 
			}
		}
		if(!encounteredFailure) {
			System.out.println("VALID URN TEST PASSED"); 
		}
		System.out.println(); 
		System.out.println("TESTING INVALID URNS TO MAKE SURE THEY FAIL TESTS..."); 
		for(int i=0; i<invalidURNS.length; i++) {
			try {
				URN urn = new URN(invalidURNS[i]);
				if(!encounteredFailure) {
					System.out.println("INVALID URN TEST FAILED");
				}
				encounteredFailure = true;
				System.out.println(); 
				System.out.println("FAILED ON URN "+i+": ");
				System.out.print(invalidURNS[i]); 
			} catch(IOException e) {
			}
		}
		if(!encounteredFailure) {
			System.out.println("INVALID URN TEST PASSED"); 
		}
		
		// TESTS FOR URN CONSTRUCTION FROM FILES, WITH SHA1 CALCULATION
		encounteredFailure = false;
		System.out.println(); 
		System.out.println("TESTING SHA1 URN CONSTRUCTION AND ASSOCIATED METHODS..."); 
		

		File[] testFiles = new File("C:\\My Music").listFiles();
		File curFile = null;
		try {
			for(int i=0; i<10; i++) {
				curFile = testFiles[i];
				if(!curFile.isFile()) {
					System.out.println("FILE NOT A FILE: "+curFile); 
				}
				URN urn = URNFactory.createSHA1URN(curFile);
				if(!urn.isSHA1()) {
					System.out.println("SHA1 TEST FAILED ON FILE: "+curFile); 
				}
				if(!urn.isURN(urn.getURNString())) {
					System.out.println("VALID URN TEST FAILED ON FILE: "+curFile); 
				}
				if(!urn.getTypeString().equals(URN.URN_SHA1 +":")) {
					System.out.println("GET TYPE STRING FAILED: "+urn); 
				}
				try {
					URN newURN = new URN(urn.toString());
					if(!newURN.equals(urn)) {
						System.out.println("ERROR IN EQUALS OR URN CONSTRUCTION FOR: "+
										   urn); 
					}
				} catch(IOException e) {
						System.out.println("ERROR IN EQUALS OR URN CONSTRUCTION FOR: "+
										   urn); 
				}
			}
		} catch(IOException e) {
			encounteredFailure = true;
			System.out.println("TEST FAILED ON FILE: "+curFile); 
		}

		if(!encounteredFailure) {
			System.out.println("SHA1 URN CONSTRUCTION AND ASSOCIATED METHODS TEST PASSED"); 
		}

		// TEST FOR isURNType method
		encounteredFailure = false;
		System.out.println(); 
		System.out.println("TESTING isURNType METHOD...");
		String[] validURNTypes = {
			"urn:",
			"urn:sha1:",
			"Urn:",
			"urn:Sha1:"
		};

		String[] invalidURNTypes = {
			"urn: ",
			"urn: sha1:",
			"urn::",
			"urn:sha2:",
			" urn:sha1",
			"rn:sha1",
			" "
		};
		
		for(int i=0; i<validURNTypes.length; i++) {
			if(!URN.isURNType(validURNTypes[i])) {
				System.out.println("isURNType failed for valid type: "+i+
								   " "+validURNTypes[i]); 
			}
		}

		for(int i=0; i<invalidURNTypes.length; i++) {
			if(URN.isURNType(invalidURNTypes[i])) {
				encounteredFailure = true;
				System.out.println("isURNType failed for invalid type: "+i+
								   " "+invalidURNTypes[i]); 
			}
		}

		if(!encounteredFailure) {
			System.out.println("isURNType METHOD TEST PASSED"); 
		}

		// ALL TESTS HAVE PASSED
		System.out.println(); 
		if(!encounteredFailure) {
			System.out.println("ALL TESTS PASSED"); 
		}
	}
	*/
	
}
