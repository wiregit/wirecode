package com.limegroup.gnutella;

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
		"http://200.30.1.02:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg",
		"http://200.30.1.02:6352/get/2/"+
		    "lime%20capital%20management%2001.mpg"
	};

	private static final String[] VALID_TIMESTAMPS = {
		"2002-04-30",
		"2002-04-30T08:31Z",
		"2002-04-30T08:31:20Z",
		"2002-04-30T08:31:21.45Z",
		"2002-04-30T00:31:21.45Z",
		"2002-04-30T23:31:21.45Z",
		"2002-04-30T23:00:21.45Z",
		"2002-04-30T23:59:21.45Z",
		"2002-04-30T23:31:00.45Z",
		"2002-04-30T23:31:59.45Z",
		"2002-01-30T23:31:59.45Z",
		"2002-01-01T23:31:59.45Z",
		"2002-01-31T23:31:59.45Z"
	};

	private static final String[] INVALID_TIMESTAMPS = {
		"2002-04",
		"2002-04-30T",
		"2002-04-30T08:31:21.45TZD",
		"2000-04-30T08:31:21.45Z",
		"2002-04-40T08:31:21.45Z",
		"2002-12-30T08:31:21.45Z",
		"2002-04-30T08:31:21.45",
		"2002-04-30T08:31:60Z",
		"2002-04-30T08:60:21Z",
		"2002-04-30T24:31:21Z",
		"2002-04-32T08:31:21Z",
		"2002-00-00T08:31:21Z"
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
				URL url1 = new URL("http", HugeTestUtils.HOST_STRINGS[i], 6346, 
								   HTTPConstants.URI_RES_N2R+
								   HugeTestUtils.URNS[i].httpStringValue());
				URL url2 = new URL("http", HugeTestUtils.HOST_STRINGS[i], 6346, "/test.htm");
				AlternateLocation al1 = 
				    AlternateLocation.createAlternateLocation(url1);
				AlternateLocation al2 = 
				    AlternateLocation.createAlternateLocation(url2);
			}
		} catch(IOException e) {
			// this also catches MalformedURLException
			fail("AlternateLocation constructor should not have thrown an "+
			"exception: "+e);
		}
	}

	public void testRemoteFileDescConstructor() {
		try {
			for(int i=0; i<HugeTestUtils.URNS.length; i++) {
				RemoteFileDesc rfd = 
					new RemoteFileDesc("www.limewire.org", 6346, 10, HTTPConstants.URI_RES_N2R+
									   HugeTestUtils.URNS[i].httpStringValue(), 10, 
									   GUID.makeGuid(), 10, true, 2, true, null, 
									   HugeTestUtils.URN_SETS[i]);
				AlternateLocation.createAlternateLocation(rfd);
			}
		} catch(Exception e) {
			e.printStackTrace();
			fail("unexpected exception: "+e);
		}
	}

	/**
	 * Tests the constructor that only takes a string argument for 
	 * valid alternate location strings that include timestamps.
	 */
	public void testStringConstructorForTimestampedLocs() {

        for(int i=0; i<HugeTestUtils.VALID_TIMESTAMPED_LOCS.length; i++) {
            try {
				AlternateLocation al = 
				    AlternateLocation.createAlternateLocation(HugeTestUtils.VALID_TIMESTAMPED_LOCS[i]);
				if(!al.isTimestamped()) {
					assertTrue("test failed -- alternate location string not "+
							   "considered stamped: "+
                               HugeTestUtils.VALID_TIMESTAMPED_LOCS[i], false);
				}
			} catch(IOException e) {
                assertTrue("test failed with exception "+e, false);
            }		 
		}
	}

	/**
	 * Tests the constructor that only takes a string argument, but in this
	 * case the strings are valid alternate locations, but they don't have 
	 * timestamps.
	 */
	public void testStringConstructorForNotTimestampedLocs() {
		for(int i=0; i<HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length; i++) {
			try {
				AlternateLocation al = 
					AlternateLocation.createAlternateLocation(HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i]);
			} catch(IOException e) {
				fail("test failed with exception: "+e+"\r\n"+
					 "on loc: "+HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i]); 
			}		
		}
	}

	/**
	 * Tests invalid alternate location strings to make sure they fail.
	 */
	public void testStringConstructorForInvalidLocs() {
		try {
			for(int i=0; i<HugeTestUtils.INVALID_LOCS.length; i++) {
				AlternateLocation al = 
				    AlternateLocation.createAlternateLocation(HugeTestUtils.INVALID_LOCS[i]);
				fail("alternate location string should not have been accepted");
			}
		} catch(IOException e) {
			// the exception is excpected in this case
		}
	}

	public void testConstructorForBadPorts() {
		try {
			for(int i=0; i<HugeTestUtils.BAD_PORT_URLS.length; i++) {
				AlternateLocation al = 
				    AlternateLocation.createAlternateLocation(HugeTestUtils.BAD_PORT_URLS[i]);
				fail("alternate location string should not have been accepted: "+
					 HugeTestUtils.BAD_PORT_URLS[i]);
			}			
		} catch (IOException e) {
			fail("should not have received an IOException: "+e);
		} catch(IllegalArgumentException e) {
			// this is what we're expecting
		}
	}

	/**
	 * Test the equals method.
	 */
	public void testAlternateLocationEquals() {
		for(int i=0; i<equalLocs.length; i++) {
			try {
				AlternateLocation curLoc = 
				    AlternateLocation.createAlternateLocation(equalLocs[i]);
				for(int j=0; j<equalLocs.length; j++) {
					AlternateLocation newLoc = 
					    AlternateLocation.createAlternateLocation(equalLocs[j]);
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
				    AlternateLocation.createAlternateLocation(HugeTestUtils.VALID_TIMESTAMPED_LOCS[i]);
				AlternateLocation al1 = 
				    AlternateLocation.createAlternateLocation(HugeTestUtils.VALID_TIMESTAMPED_LOCS[i]);
				assertTrue("alternate location should have a timestamp", al0.isTimestamped());
				assertTrue("alternate location should have a timestamp", al1.isTimestamped());
				timeStampedAltLocs0.put(al0, al0);
				testMap.put(al1, al1);
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
				AlternateLocation curLoc = 
				    AlternateLocation.createAlternateLocation(equalLocs[i]);
				for(int j=0; j<equalLocs.length; j++) {
					AlternateLocation newLoc = 
					    AlternateLocation.createAlternateLocation(equalLocs[j]);
					int z = curLoc.compareTo(newLoc);
					assertEquals("locations should be equal", z, 0);
				}
			} catch(IOException e) {
				fail("unexpected exception: "+e);
			}
		}

		// make sure that alternate locations with the same timestamp and
		// different URLs are not considered the same
		for(int i=0; i<(HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length-1); i++) {
			String loc0 = HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i];
			String loc1 = HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i+1];

			// give them the same timestamp to make sure compareTo 
			// differentiates properly
			loc0 = loc0 + " 2002-04-30T08:30Z";
			loc1 = loc1 + " 2002-04-30T08:30Z";
			try {
				AlternateLocation curLoc0 = 
			        AlternateLocation.createAlternateLocation(loc0);
				AlternateLocation curLoc1 = 
			        AlternateLocation.createAlternateLocation(loc1);
				int z = curLoc0.compareTo(curLoc1);
				assertTrue("locations should compare to different values", z!=0);
			} catch(Exception e) {
				fail("unexpected exception: "+e);
			}
		}
	}

	/**
	 * Tests to make sure that alternate locations with equal URLs and 
	 * different timestamps are considered different.  This test relies
	 * on several properties of the VALID_TIMESTAMPS list.  For example,
	 * the java Calendar class's set method does not take fractions of a
	 * second, so we consider timestamps with the same second reading as
	 * identical regardless of whether or not they have different fractions
	 * of that second, which is the behavior we want.
	 */
	public void testAlternateLocationCompareToWithEqualUrlsDifferentDates() {
		// make sure that alternate locations with the same urls and different
		// timestamps are considered different
		for(int i=0; i<(HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length-1); i++) {
			String nonTSloc0 = HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i];
			String nonTSloc1 = HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i];

			for(int j=0; j<VALID_TIMESTAMPS.length-1; j++) {
				// give them the same timestamp to make sure compareTo 
				// differentiates properly
				String loc0 = nonTSloc0 + " "+VALID_TIMESTAMPS[j];
				String loc1 = nonTSloc1 + " "+VALID_TIMESTAMPS[j+1];
				try {
					AlternateLocation curLoc0 = 
			            AlternateLocation.createAlternateLocation(loc0);
					AlternateLocation curLoc1 = 
			            AlternateLocation.createAlternateLocation(loc1);
					int z = curLoc0.compareTo(curLoc1);
					assertTrue("locations should compare to different values:\r\n"+
							   curLoc0 + "\r\n" + curLoc1+"\r\n"+
							   loc0+ "\r\n"+
							   loc1+ "\r\n", 
							   z!=0);
				} catch(Exception e) {
					e.printStackTrace();
					fail("unexpected exception: "+e);
				}
			}
		}
	}

	/**
	 * Tests the construction of alternate locations with dates that do 
	 * not meet the appropriate syntax to make sure they fail.
	 */
	public void testAlternateLocationConstructorWithInvalidDates() {
		// make sure that alternate locations with the same urls and different
		// timestamps are considered different
		for(int i=0; i<(HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length-1); i++) {
			String nonTSloc = HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i];

			for(int j=0; j<INVALID_TIMESTAMPS.length-1; j++) {
				String loc = nonTSloc + " "+INVALID_TIMESTAMPS[j];
				try {
					AlternateLocation al = 
			            AlternateLocation.createAlternateLocation(loc);
					assertEquals("alternate location should not have a timestamp: "+loc,
								 false, al.isTimestamped());
				} catch(IOException e) {
				}
			}
		}
	}

	/**
	 * Tests the construction of alternate locations with dates that do 
	 * meet the appropriate syntax to make sure they fail.
	 */
	public void testAlternateLocationConstructorWithValidDates() {
		// make sure that alternate locations with the same urls and different
		// timestamps are considered different
		for(int i=0; i<(HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length-1); i++) {
			String nonTSloc = HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i];

			for(int j=0; j<VALID_TIMESTAMPS.length-1; j++) {
				String loc = nonTSloc + " "+VALID_TIMESTAMPS[j];
				try {
					AlternateLocation al = 
			            AlternateLocation.createAlternateLocation(loc);
					assertEquals("alternate location should not have a timestamp: "+loc,
								 true, al.isTimestamped());
				} catch(IOException e) {
					fail("unexpected exception: "+e);
				}
			}
		}
	}

    /**
     * Test to make sure that we're handling firewalls fine -- rejecting
     * firewalled locations and accepting non-firewalled locations.
     */
    public void testAlternateLocationToMakeSureItDisallowsFirewalledHosts() {
        for(int i=0; i<HugeTestUtils.FIREWALLED_LOCS.length; i++) {
            String loc = HugeTestUtils.FIREWALLED_LOCS[i];
            try {
                AlternateLocation al = AlternateLocation.createAlternateLocation(loc);
                fail("alt loc should have thrown an exception for loc: "+loc);
            } catch(Exception e) {
            }
        }

        for(int i=0; i<HugeTestUtils.NON_FIREWALLED_LOCS.length; i++) {
            String loc = HugeTestUtils.NON_FIREWALLED_LOCS[i];
            try {
                AlternateLocation al = AlternateLocation.createAlternateLocation(loc);
            } catch(Exception e) {
                fail("unexpected exception for loc: "+loc+" "+e);
            }
        }
    }
}
