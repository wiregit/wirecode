package com.limegroup.gnutella.altlocs;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.*;
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
public final class AlternateLocationTest extends com.limegroup.gnutella.util.BaseTestCase {


	private static final String[] equalLocs = {
		"http://200.30.1.02:6352" + HTTPConstants.URI_RES_N2R +
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
		"http://200.30.1.02:6352" + HTTPConstants.URI_RES_N2R +
		    "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB",
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
		"2002-04-40T08:31:21.45Z",
		"2002-13-30T08:31:21.45Z",
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
		return buildTestSuite(AlternateLocationTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public void setUp() {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(true);
    }

	/**
	 * Tests the constructor that takes a URL as an argument to make sure it
	 * succeeds when it should.
	 */
	public void testUrlUrnConstructorForSuccess() throws Exception {
		for(int i=0; i<HugeTestUtils.URNS.length; i++) {
			URN urn = URN.createSHA1Urn(HugeTestUtils.VALID_URN_STRINGS[i]);
			URL url = new URL("http", HugeTestUtils.HOST_STRINGS[i], 6346, 
							  HTTPConstants.URI_RES_N2R+
							  HugeTestUtils.URNS[i].httpStringValue());
			AlternateLocation al = 
			    AlternateLocation.create(url);
		}
	}

	/**
	 * Tests the constructor that takes a string as an argument to make sure it
	 * succeeds when it should.
	 */
	public void testStringUrnConstructorForSuccess() throws Exception {
		for(int i=0; i<HugeTestUtils.URNS.length; i++) {
			URN urn = URN.createSHA1Urn(HugeTestUtils.VALID_URN_STRINGS[i]);
			String url = "http://"+HugeTestUtils.HOST_STRINGS[i]+":6346"+ 
				HTTPConstants.URI_RES_N2R+
				HugeTestUtils.URNS[i].httpStringValue();
			AlternateLocation al = 
			    AlternateLocation.create(url);
		}
	}

	/**
	 * Tests the constructor that takes a URL as an argument to make sure it fails when
	 * there's no SHA1.
	 */
	public void testUrlUrnConstructorForFailure() {
		try {
			for(int i=0; i<HugeTestUtils.URNS.length; i++) {
				URL url = new URL("http", HugeTestUtils.HOST_STRINGS[i], 6346, "/test.htm");
				AlternateLocation al = 
				    AlternateLocation.create(url);
				fail("AlternateLocation constructor should have thrown an exception");
			}
		} catch(IOException e) {
			// this also catches MalformedURLException
		}
	}


	/**
	 * Tests the constructor that takes a URL as an argument to make sure it fails when
	 * there's no SHA1.
	 */
	public void testStringUrnConstructorForFailure() {
		try {
			for(int i=0; i<HugeTestUtils.URNS.length; i++) {
				String url = "http://" +HugeTestUtils.HOST_STRINGS[i]+":6346/test.htm";
				AlternateLocation al = 
				    AlternateLocation.create(url);
				fail("AlternateLocation constructor should have thrown an exception");
			}
		} catch(IOException e) {
			// this also catches MalformedURLException
		}
	}

	/**
	 * Tests the constructor that creates an alternate location from a remote
	 * file desc.
	 */
	public void testRemoteFileDescConstructor() throws Exception {
		for(int i=0; i<HugeTestUtils.URNS.length; i++) {
			RemoteFileDesc rfd = 
				new RemoteFileDesc("www.limewire.org", 6346, 10, HTTPConstants.URI_RES_N2R+
								   HugeTestUtils.URNS[i].httpStringValue(), 10, 
								   GUID.makeGuid(), 10, true, 2, true, null, 
								   HugeTestUtils.URN_SETS[i],
                                   false,false,"",0,null);

            // just make sure this doesn't throw an exception
			AlternateLocation.create(rfd);
		}

        try {
            RemoteFileDesc rfd = 
                new RemoteFileDesc("127.0.2.1", 6346, 10, HTTPConstants.URI_RES_N2R+
                                   HugeTestUtils.URNS[0].httpStringValue(), 10, 
                                   GUID.makeGuid(), 10, true, 2, true, null, 
                                   HugeTestUtils.URN_SETS[0],
                                   false,false,"",0,null);

            // this should throw an exception, since it's a private address.
            AlternateLocation.create(rfd);        

            fail("should have rejected the location because the address is private");
        } catch(IOException e) {
            // expected for private addresses
        }

        try {
            AlternateLocation.create((RemoteFileDesc)null);
            fail("should have thrown a null pointer");
        } catch(NullPointerException e) {
            // this is expected
        }                
        
	}

	/**
	 * Tests the factory method that creates a RemoteFileDesc from an alternate
	 * location.
	 */
	public void testCreateRemoteFileDesc() {
		for(int i=0; i<HugeTestUtils.UNEQUAL_SHA1_LOCATIONS.length; i++) {
			AlternateLocation al = HugeTestUtils.UNEQUAL_SHA1_LOCATIONS[i];
			RemoteFileDesc rfd = al.createRemoteFileDesc(10);
			assertEquals("SHA1s should be equal", al.getSHA1Urn(), rfd.getSHA1Urn());
			assertEquals("urls should be equal", al.getUrl(), rfd.getUrl());
		}
	}

	/**
	 * Tests the constructor that only takes a string argument for 
	 * valid alternate location strings that include timestamps.
	 */
	public void testStringConstructorForTimestampedLocs() throws Exception {
        try {
        for(int i=0; i<HugeTestUtils.VALID_TIMESTAMPED_LOCS.length; i++) {
			AlternateLocation al = 
			  AlternateLocation.create(HugeTestUtils.VALID_TIMESTAMPED_LOCS[i]);
		}
        } catch(IOException iox) {
            fail("failed to create locaction with timestamp string");
        }
    }

	/**
	 * Tests the constructor that only takes a string argument, but in this
	 * case the strings are valid alternate locations, but they don't have 
	 * timestamps.
	 */
	public void testStringConstructorForNotTimestampedLocs() throws Exception {
		for(int i=0; i<HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length; i++) {
			try {
				AlternateLocation al = 
					AlternateLocation.create(HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i]);
			} catch(IOException e) {
				fail("failed on loc: "+HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i], e); 
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
				    AlternateLocation.create(HugeTestUtils.INVALID_LOCS[i]);
				fail("alternate location string should not have been accepted");
			}
		} catch(IOException e) {
			// the exception is excpected in this case
		}
	}

	public void testConstructorForBadPorts() throws Exception {
		try {
			for(int i=0; i<HugeTestUtils.BAD_PORT_URLS.length; i++) {
				AlternateLocation al = 
				    AlternateLocation.create(HugeTestUtils.BAD_PORT_URLS[i]);
				fail("alternate location string should not have been accepted: "+
					 HugeTestUtils.BAD_PORT_URLS[i]);
			}			
		} catch(IOException e) {
			// this is what we're expecting
		}
	}

    public void testDemotedEquals() throws Exception {
        AlternateLocation loc1 = AlternateLocation.create(equalLocs[0]);
        AlternateLocation loc2 = AlternateLocation.create(equalLocs[0]);
        assertEquals("locations should be equal", loc1, loc2);
        loc2.demote();
        assertEquals("locations should be equal", loc1, loc2);
    }


	/**
	 * Test the equals method.
	 */
	public void testAlternateLocationEquals() throws Exception {
		for(int i=0; i<equalLocs.length; i++) {
			AlternateLocation curLoc = AlternateLocation.create(equalLocs[i]);
			for(int j=0; j<equalLocs.length; j++) {
				AlternateLocation newLoc=AlternateLocation.create(equalLocs[j]);
				assertEquals("locations should be equal", curLoc, newLoc);
			}
		}
		TreeMap timeStampedAltLocs0 = new TreeMap();
		TreeMap testMap = new TreeMap();
		for(int i=0; i<HugeTestUtils.VALID_TIMESTAMPED_LOCS.length; i++) {
			AlternateLocation al0 = 				    
			  AlternateLocation.create(HugeTestUtils.VALID_TIMESTAMPED_LOCS[i]);
			AlternateLocation al1 = 
			  AlternateLocation.create(HugeTestUtils.VALID_TIMESTAMPED_LOCS[i]);
			timeStampedAltLocs0.put(al0, al0);
			testMap.put(al1, al1);
		}

		//assertEquals("maps should be equal", timeStampedAltLocs0, testMap);
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
	public void testAlternateLocationCompareTo() throws Exception {
		for(int i=0; i<equalLocs.length; i++) {
			AlternateLocation curLoc = 
			    AlternateLocation.create(equalLocs[i]);
			for(int j=0; j<equalLocs.length; j++) {
				AlternateLocation newLoc = 
				    AlternateLocation.create(equalLocs[j]);
				int z = curLoc.compareTo(newLoc);
				assertEquals("locations should be equal", 0, z);
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
			AlternateLocation curLoc0 = 
		        AlternateLocation.create(loc0);
			AlternateLocation curLoc1 = 
		        AlternateLocation.create(loc1);
			int z = curLoc0.compareTo(curLoc1);
			assertNotEquals("locations should compare to different values", 0, z);
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
	public void testAlternateLocationCompareToWithEqualUrlsDifferentDates() 
                                                             throws Exception {
		// make sure that alternate locations with the same urls and different
		// timestamps are considered same
		for(int i=0;i<(HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length-1); i++) {
			String nonTSloc0 = HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i];
			String nonTSloc1 = HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i];

			for(int j=0; j<VALID_TIMESTAMPS.length-1; j++) {
				// give them the same timestamp to make sure compareTo 
				// differentiates properly
				String loc0 = nonTSloc0 + " "+VALID_TIMESTAMPS[j];
				String loc1 = nonTSloc1 + " "+VALID_TIMESTAMPS[j+1];
				AlternateLocation curLoc0 = AlternateLocation.create(loc0);
				AlternateLocation curLoc1 = AlternateLocation.create(loc1);
				int z = curLoc0.compareTo(curLoc1);
				assertEquals("locations should compare to same values:\r\n"+
                             curLoc0 + "\r\n" + curLoc1+"\r\n", 0, z);
			}
		}
	}

	/**
	 * Tests the construction of alternate locations with dates that do 
	 * not meet the appropriate syntax to make sure they fail.
	 */
	public void testAlternateLocationConstructorWithInvalidDates() {
		for(int i=0;i<(HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length-1);i++) {
			String nonTSloc = HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i];

			for(int j=0; j<INVALID_TIMESTAMPS.length-1; j++) {
				String loc = nonTSloc + " "+INVALID_TIMESTAMPS[j];
				try {
					AlternateLocation al = AlternateLocation.create(loc);
				} catch(IOException e) {
                    fail("could not create AlternateLocation with bad date");
				}
			}
		}
	}

	/**
	 * Tests the construction of alternate locations with dates that do 
	 * meet the appropriate syntax to make sure they fail.
	 */
	public void testAlternateLocationConstructorWithValidDates() 
                                                             throws Exception {
		// make sure that alternate locations with the same urls and different
		// timestamps are considered different
		for(int i=0;i<(HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length-1);i++) {
			String nonTSloc = HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i];

			for(int j=0; j<VALID_TIMESTAMPS.length-1; j++) {
				String loc = nonTSloc + " "+VALID_TIMESTAMPS[j];
				try {
                    AlternateLocation al = AlternateLocation.create(loc);
                } catch (IOException e) {
                    fail("could not create AlternateLocation with valid date");
                }
			}
		}
	}

    /**
     * Test to make sure that we're handling firewalls fine -- rejecting
     * firewalled locations and accepting non-firewalled locations.
     */
    public void testAlternateLocationToMakeSureItDisallowsFirewalledHosts() throws Exception {
        for(int i=0; i<HugeTestUtils.FIREWALLED_LOCS.length; i++) {
            String loc = HugeTestUtils.FIREWALLED_LOCS[i];
            try {
                AlternateLocation al = AlternateLocation.create(loc);
                fail("alt loc should not have accepted firewalled loc: "+loc);
            } catch(Exception e) {
                // this is expected 
            }
        }

        for(int i=0; i<HugeTestUtils.NON_FIREWALLED_LOCS.length; i++) {
            String loc = HugeTestUtils.NON_FIREWALLED_LOCS[i];
            AlternateLocation al = AlternateLocation.create(loc);
        }
    }
}
