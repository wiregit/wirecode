package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;

/**
 * This class handles testing all methods of the urn class.
 */
public final class UrnTest extends TestCase {
	
	private static final String [] VALID_URNS = {
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"Urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"uRn:sHa1:PLRTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLPTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:Sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"UrN:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sHa1:PLSTIIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTXIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTTIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:bitprint:XLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
		             "PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567",
		"urn:Bitprint:RLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
		             "PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567"
	};
	
	private static final String [] INVALID_URNS = {
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

	private static final String [] VALID_SHA1_URNS = {
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"Urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGZQYPFB",
		"uRn:sHa1:PLRTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLPTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:Sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"UrN:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sHa1:PLSTIIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTXIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"urn:sha1:PLSTTIPQGSSZTS5FJUPAKUZWUGYQYPFB"
	};

	private static URN[] urns;
	private static URN[] sha1Urns;

	public UrnTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(UrnTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() {
		urns = new URN[VALID_URNS.length];
		for(int i=0; i<urns.length; i++) {
			try {
				urns[i] = URNFactory.createUrn(VALID_URNS[i]);
				assertTrue(urns[i] != null);
			} catch(IOException e) {
				assertTrue(false);
			}		
		}
		sha1Urns = new URN[VALID_SHA1_URNS.length];
		for(int i=0; i<sha1Urns.length; i++) {
			try {
				sha1Urns[i] = URNFactory.createUrn(VALID_SHA1_URNS[i]);
				assertTrue(sha1Urns[i] != null);
			} catch(IOException e) {
				assertTrue(false);
			}		
		}
	}

	public void testValidUrns() {
		for(int i=0; i<VALID_URNS.length; i++) {
			try {
				URN urn = URNFactory.createUrn(VALID_URNS[i]);
			} catch(IOException e) {
				assertTrue(false);				
			}
		}
	}


	public void testInvalidUrns() {
		boolean encounteredFailure = false;
		for(int i=0; i<INVALID_URNS.length; i++) {
			try {
				URN urn = URNFactory.createUrn(INVALID_URNS[i]);
				assertTrue(false);
			} catch(IOException e) {
			}
		}
	}

	public void testUrnConstructionFromFiles() {
		// TESTS FOR URN CONSTRUCTION FROM FILES, WITH SHA1 CALCULATION
		File[] testFiles = new File("gui/lib").listFiles();
		File curFile = null;
		try {
			for(int i=0; i<10; i++) {
				curFile = testFiles[i];
				if(!curFile.isFile()) {
					continue;
				}
				URN urn = URNFactory.createSHA1Urn(curFile);
				assertTrue(urn.isSHA1());
				assertTrue(urn.isUrn(urn.stringValue()));
				assertTrue(urn.getTypeString().equals(URN.URN_SHA1+":"));
				try {
					URN newURN = URNFactory.createUrn(urn.toString());
					assertTrue(newURN.equals(urn));
				} catch(IOException e) {
					assertTrue(false);
				}
			}
		} catch(IOException e) {
			assertTrue(false);
		}
	}

	public void testIsUrnTypeMethod() {
		// TEST FOR isURNType method
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
			assertTrue(URN.isUrnType(validURNTypes[i]));
		}

		for(int i=0; i<invalidURNTypes.length; i++) {
			assertTrue(!URN.isUrnType(invalidURNTypes[i]));
		}
	}


	public void testHashCode() {
		int[] hashCodes = new int[VALID_URNS.length];
		for(int i=0; i<VALID_URNS.length; i++) {
			try {
				hashCodes[i] = URNFactory.createUrn(VALID_URNS[i]).hashCode();
			} catch(IOException e) {
				assertTrue(false);
			}
		}

		for(int i=0; i<hashCodes.length; i++) {
			int curCode = hashCodes[i];
			for(int j=0; j<hashCodes.length; j++) {
				if(i == j) continue;
				assertTrue(curCode != hashCodes[j]);
			}
		}
	}

	public void testEquals() {
		URN curUrn;
		for(int i=0; i<urns.length; i++) {
			curUrn = urns[i];
			assertTrue(curUrn != null);
			for(int j=0; j<urns.length; j++) {
				if(i == j) {
					try {
						URN tempUrn = URNFactory.createUrn(urns[j].stringValue());
						assertTrue(curUrn.equals(tempUrn));
					} catch(IOException e) {
						assertTrue(false);
					}
					continue;
				}
				assertTrue(urns[j] != null);
				assertTrue(!curUrn.equals(urns[j]));
			}
		}
	}

	public void testIsSHA1Method() {
		for(int i=0; i<sha1Urns.length; i++) {
			assertTrue(sha1Urns[i].isSHA1());
		}
	}
	
}
