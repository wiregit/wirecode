package com.limegroup.gnutella.tests;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;
import java.util.*;
import java.net.*;

/**
 * This class tests the methods of the <tt>AlternateLocation</tt> class.
 */
public final class AlternateLocationTest extends TestCase {
	private static final String[] validTimestampedLocs = {
		"Alternate-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z",
		"Alt-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z",
		"Alt-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z",
		"X-Gnutella-Alternate-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z",
		"http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z",
		"http: //Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z"
	};

	private static final String[] validlocs = {
		"Alternate-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg",
		"Alt-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg",
		"Alt-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg",
		"X-Gnutella-Alternate-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg",
		"http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg",
		"http: //Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg"
	};
	
	
	private static final String [] validURNS = {
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

	private static final String [] validURLS = {
		"www.limewire.org",
		"www.limewire.org",
		"www.limewire.org",
		"www.limewire.org",
		"www.limewire.org",
		"www.limewire.org",
		"www.limewire.org",
		"www.limewire.org",
		"www.limewire.org",
		"www.limewire.org",
		"www.limewire.org"
	};

	public AlternateLocationTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(AlternateLocationTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Tests the constructor that takes a URL and a URN as arguments.
	 */
	public void testUrlUrnConstructor() {
		try {
			for(int i=0; i<validURNS.length; i++) {
				URN urn = new URN(validURNS[i]);
				URL url1 = new URL("http", validURLS[i], 6346, 
								   URNFactory.createHttpUrnFileString(urn));
				URL url2 = new URL("http", validURLS[i], "/test.htm");
				AlternateLocation al1 = new AlternateLocation(url1);
				AlternateLocation al2 = new AlternateLocation(url2);
				AlternateLocation al3 = 
				    new AlternateLocation("http://"+validURLS[i] + ":6346"+
										  URNFactory.createHttpUrnFileString(urn)+
										  " "+AlternateLocation.convertDateToString(new Date()));
				AlternateLocation al4 = 
				    new AlternateLocation("http://"+validURLS[i] + "/test.htm"+
										  " "+AlternateLocation.convertDateToString(new Date()));
				URL urlTest1 = al1.getUrl();
				URL urlTest2 = al2.getUrl();
				String dateStr1 = "2002-04-09T20:32:33Z";
				String dateStr2 = "2002-04-09T20:32:33Z";
				assertEquals(al1, al3);
				assertEquals(al2, al4);
				assertEquals(url1, urlTest1);
				assertEquals(url2, urlTest2);
				assertEquals(dateStr1, dateStr2);
			}
		} catch(IOException e) {
			// this also catches MalformedURLException

			assertTrue(false);
			System.out.println("TEST FAILED WITH EXCEPTION: "); 
			System.out.println(e); 
			e.printStackTrace();
		}
	}

	/**
	 * Tests the constructor that only takes a string argument for 
	 * valid alternate location strings that include timestamps.
	 */
	public void testStringConstructorForTimestampedLocs() {
		try {
			for(int i=0; i<validTimestampedLocs.length; i++) {
				AlternateLocation al = new AlternateLocation(validTimestampedLocs[i]);
				if(!AlternateLocation.isTimestamped(validTimestampedLocs[i])) {
					assertTrue("test failed -- alternate location string not "+
							   "considered stamped", false);
				}
				if(!al.isTimestamped()) {
					assertTrue("test failed -- alternate location string not "+
							   "considered stamped", false);
				}
			}
		} catch(IOException e) {
			assertTrue("test failed with exception "+e, false);
		}		
	}

	/**
	 * Tests the constructor that only takes a string argument, but in this
	 * case the strings are valid alternate locations, but they don't have 
	 * timestamps.
	 */
	public void testStringConstructorForNotTimestampedLocs() {
		try {
			for(int i=0; i<validlocs.length; i++) {
				AlternateLocation al = new AlternateLocation(validlocs[i]);
			}
		} catch(IOException e) {
			assertTrue("test failed with exception: "+e, false); 
			e.printStackTrace();
		}		
	}

	/**
	 * Test the date-related methods.
	 */
	public void testDateMethods() {
		Date date = new Date();
		String dateStr = AlternateLocation.convertDateToString(date);
		if(!AlternateLocation.isValidDate(dateStr)) {
			assertTrue("test failed: valid date not considered valid\r\ndate: "+date+
					   "\r\ndate string: "+dateStr, false);
		}	   
	}
}
