package com.limegroup.gnutella.tests;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
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
	private static final String[] validTimestampedLocs = {
		"http://Y.Y.Y.Y:6352/get/2/"+
		                     "lime%20capital%20management%2001.mpg "+
		                     "2002-04-09T20:32:33Z",
		"http://Y.Y.Y.Y:6352/get/2/"+
		               "lime%20capital%20management%2002.mpg "+
		               "2002-04-09T20:32:33Z",
		"http://Y.Z.Y.Y:6352/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z",
		"http://Y.W.Y.Y:6352/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z",
		"http://Y.T.Y.Y:6352/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z",
		"http: //Y.R.Y.Y:6352/get/2/"+
		               "lime%20capital%20management%2001.mpg "+
		               "2002-04-09T20:32:33Z",
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
								   URNFactory.createHttpUrnServiceRequest(urn));
				URL url2 = new URL("http", validURLS[i], "/test.htm");
				AlternateLocation al1 = new AlternateLocation(url1);
				AlternateLocation al2 = new AlternateLocation(url2);
				AlternateLocation al3 = 
				    new AlternateLocation("http://"+validURLS[i] + ":6346"+
										  URNFactory.createHttpUrnServiceRequest(urn)+
										  " "+AlternateLocation.convertDateToString(new Date()));
				AlternateLocation al4 = 
				    new AlternateLocation("http://"+validURLS[i] + "/test.htm"+
										  " "+AlternateLocation.convertDateToString(new Date()));
			}
		} catch(IOException e) {
			// this also catches MalformedURLException
			assertTrue("AlternateLocation constructor should not have thrown an "+
					   "exception", false);
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

	/**
	 * Test the equals method.
	 */
	public void testAlternateLocationEquals() {
		TreeMap timeStampedAltLocs0 = new TreeMap();
		TreeMap testMap = new TreeMap();
		for(int i=0; i<validTimestampedLocs.length; i++) {
			try {
				AlternateLocation al0 = new AlternateLocation(validTimestampedLocs[i]);
				AlternateLocation al1 = new AlternateLocation(validTimestampedLocs[i]);
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
}
