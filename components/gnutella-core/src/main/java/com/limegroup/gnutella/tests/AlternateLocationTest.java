package com.limegroup.gnutella.tests;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.CommonUtils;
import junit.framework.*;
import junit.extensions.*;
import java.io.*;
import java.util.Date;
import java.net.*;

/**
 * This class tests the methods of the <tt>AlternateLocation</tt> class.
 */
public final class AlternateLocationTest extends TestCase {


	private static final String[] equalLocs = {
		HTTPHeaderName.ALT_LOCATION.httpStringValue()+
		    ": http://Y.Y.Y.Y:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://Y.Y.Y.Y:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg"
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
			for(int i=0; i<HugeTestUtils.URNS.length; i++) {
				URN urn = URN.createSHA1Urn(HugeTestUtils.VALID_URN_STRINGS[i]);
				URL url1 = new URL("http", HugeTestUtils.URL_STRINGS[i], 6346, 
								   "/uri-res/N2R?"+HugeTestUtils.URNS[i].httpStringValue());
				URL url2 = new URL("http", HugeTestUtils.URL_STRINGS[i], "/test.htm");
				AlternateLocation al1 = new AlternateLocation(url1);
				AlternateLocation al2 = new AlternateLocation(url2);
				Date date = new Date();
				AlternateLocation al3 = 
				    new AlternateLocation("http://"+HugeTestUtils.URL_STRINGS[i] + ":6346"+
										  "/uri-res/N2R?"+urn.httpStringValue()+
										  " "+AlternateLocation.convertDateToString(date));
				AlternateLocation al4 = 
				    new AlternateLocation("http://"+HugeTestUtils.URL_STRINGS[i] + "/test.htm"+
										  " "+AlternateLocation.convertDateToString(date));
			}
		} catch(IOException e) {
			// this also catches MalformedURLException
			assertTrue("AlternateLocation constructor should not have thrown an "+
					   "exception: "+e, false);
		}
	}

	/**
	 * Tests the constructor that only takes a string argument for 
	 * valid alternate location strings that include timestamps.
	 */
	public void testStringConstructorForTimestampedLocs() {
		try {
			for(int i=0; i<HugeTestUtils.VALID_TIMESTAMPED_LOCS.length; i++) {
				AlternateLocation al = new AlternateLocation(HugeTestUtils.VALID_TIMESTAMPED_LOCS[i]);
				if(!AlternateLocation.isTimestamped(HugeTestUtils.VALID_TIMESTAMPED_LOCS[i])) {
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
			for(int i=0; i<HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length; i++) {
				AlternateLocation al = 
				    new AlternateLocation(HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i]);
			}
		} catch(IOException e) {
			assertTrue("test failed with exception: "+e, false); 
			e.printStackTrace();
		}		
	}

	/**
	 * Tests invalid alternate location strings to make sure they fail.
	 */
	public void testStringConstructorForInvalidLocs() {
		try {
			for(int i=0; i<HugeTestUtils.INVALID_LOCS.length; i++) {
				AlternateLocation al = 
				    new AlternateLocation(HugeTestUtils.INVALID_LOCS[i]);
				assertTrue("alternate location string should not have been accepted",
						   false);
			}
		} catch(IOException e) {
			// the exception is excpected in this case
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

	/**
	 * Test the equals method.
	 */
	public void testAlternateLocationEquals() {

		for(int i=0; i<equalLocs.length; i++) {
			try {
				AlternateLocation curLoc = new AlternateLocation(equalLocs[i]);
				for(int j=0; j<equalLocs.length; j++) {
					AlternateLocation newLoc = new AlternateLocation(equalLocs[j]);
					assertEquals("locations should be equal", curLoc, newLoc);
				}
			} catch(IOException e) {
				assertTrue("unexpected exception: "+e, false);
			}
		}
		TreeMap timeStampedAltLocs0 = new TreeMap();
		TreeMap testMap = new TreeMap();
		for(int i=0; i<HugeTestUtils.VALID_TIMESTAMPED_LOCS.length; i++) {
			try {
				AlternateLocation al0 = 
				    new AlternateLocation(HugeTestUtils.VALID_TIMESTAMPED_LOCS[i]);
				AlternateLocation al1 = 
				    new AlternateLocation(HugeTestUtils.VALID_TIMESTAMPED_LOCS[i]);
				timeStampedAltLocs0.put(al0, al0);
				testMap.put(al1, al1);
				//timeStampedAltLocs0.add(al);
			} catch(IOException e) {
				assertTrue("unexpected exception: "+e, false);
			}
		}

		assertEquals("maps should be equal", timeStampedAltLocs0, testMap);
		TreeMap timeStampedAltLocs1 = new TreeMap(timeStampedAltLocs0);
		Iterator iter0 = timeStampedAltLocs0.values().iterator();
		Iterator iter1 = timeStampedAltLocs1.values().iterator();
		while(iter0.hasNext()) {
			AlternateLocation al0 = (AlternateLocation)iter0.next();
			AlternateLocation al1 = (AlternateLocation)iter1.next();
			assertEquals("alternate location values should be equal", al0, al1);
		}

		Iterator iter20 = timeStampedAltLocs0.keySet().iterator();
		Iterator iter21 = timeStampedAltLocs1.keySet().iterator();
		while(iter20.hasNext()) {
			AlternateLocation al0 = (AlternateLocation)iter20.next();
			AlternateLocation al1 = (AlternateLocation)iter21.next();
			assertEquals("alternate location keys should be equal", al0, al1);
		}

		Iterator iter30 = timeStampedAltLocs0.entrySet().iterator();
		Iterator iter31 = timeStampedAltLocs1.entrySet().iterator();
		while(iter20.hasNext()) {
			Map.Entry al0 = (Map.Entry)iter30.next();
			Map.Entry al1 = (Map.Entry)iter31.next();
			assertEquals("alternate location entries should be equal", al0, al1);
		}

		assertEquals("sizes should be equal", timeStampedAltLocs1.size(), 
					 timeStampedAltLocs0.size());
		Iterator i = timeStampedAltLocs0.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry e = (Map.Entry) i.next();
			Object key   = e.getKey();
			Object value = e.getValue();
			if (value == null) {
				if (!(timeStampedAltLocs1.get(key)==null && 
					  timeStampedAltLocs1.containsKey(key)))
					assertTrue("key not found", false);
			} else {
				if (!value.equals(timeStampedAltLocs1.get(key)))
					assertTrue("value not found", false);
			}
		}

		// the following does not work for some reason, but I'm not at all sure 
		// why, especially considering that all of the above code passes the 
		// assertions??
		//assertEquals("maps should be equal: "+timeStampedAltLocs0, timeStampedAltLocs1);
	}

	/**
	 * Tests the compareTo method of the AlternateLocation class.
	 */
	public void testAlternateLocationCompareTo() {
		for(int i=0; i<equalLocs.length; i++) {
			try {
				AlternateLocation curLoc = new AlternateLocation(equalLocs[i]);
				for(int j=0; j<equalLocs.length; j++) {
					AlternateLocation newLoc = new AlternateLocation(equalLocs[j]);
					int z = curLoc.compareTo(newLoc);
					assertEquals("locations should be equal", z, 0);
				}
			} catch(IOException e) {
				assertTrue("unexpected exception: "+e, false);
			}
		}		
	}
}
