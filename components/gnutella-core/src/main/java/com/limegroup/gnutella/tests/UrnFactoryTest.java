package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;

/**
 * This class tests the public methods of the URNFactory class.
 */
public final class UrnFactoryTest extends TestCase {
	
	private final String [] validURNStrings = {
		"GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /URI-RES/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/n2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2r?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/n2r?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		"GET /uri-res/N2R?UrN:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R?urn:sHa1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		"GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		"GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HtTP/1.0",
		"GET /uri-res/N2R?urn:bitprint:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
		"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.0",
		"GET /uri-res/N2R?urn:bitprint:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
		"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.1"
	};

	private final URN[] validURNS = new URN[validURNStrings.length];
	
	private final String [] invalidURNStrings = {
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
	
	public UrnFactoryTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(UrnFactoryTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() {
		for(int i=0; i<validURNStrings.length; i++) {
			try {
				validURNS[i] = URNFactory.createSHA1UrnFromGetRequest(validURNStrings[0]);
			} catch(IOException e) {
				assertTrue("could not create urns for UrnFactorTest setup", false);
			}
		}
	}
		

	/**
	 * Test the constructor that constructs a URN from a URN get request.
	 */
	public void testUrnGetConstructor() {
		for(int i=0; i<validURNStrings.length; i++) {
			try {
				URNFactory.createSHA1UrnFromGetRequest(validURNStrings[i]);
			} catch(IOException e) {
				assertTrue("construction of an URN from a valid get request failed",
						   false);
			}
		}
		for(int i=0; i<invalidURNStrings.length; i++) {
			try {
				URNFactory.createSHA1UrnFromGetRequest(invalidURNStrings[i]);
				assertTrue("construction of a URN from an invalid get request succeeded",
						   false);				
			} catch(IOException e) {
				continue;
			}			
		}
	}

	
	/**
	 * Test the URNFactory method that creates a URN "service request" over 
	 * HTTP, as specified in RFC 2169.
	 */
	public void testCreateHttpUrnServiceRequest() {
		for(int i=0;i<validURNS.length; i++) {
			String str = URNFactory.createHttpUrnServiceRequest(validURNS[i]);
			try {
				URNFactory.createUrnFromServiceRequest(str);
			} catch(IOException e) {
				assertTrue("urn construction failed for generated http urn "+
						   "service request string: "+e, false);
			}
		}
	}

	/**
	 * Test the URN constructor that takes only a file parameter.
	 */
	public void testCreateSHA1Urn() {
		File gnutellaDir = new File("c:/work/lime/core/com/limegroup/gnutella");
		File[] files = gnutellaDir.listFiles();
		for(int i=0; i<files.length; i++) {
			if(!files[i].isFile()) continue;
			try {
				URNFactory.createSHA1Urn(files[i]);
			} catch(IOException e) {
				assertTrue("could not create a SHA1 URN from a valid file: "+
						   e, false);
			}
		}
	}
}
