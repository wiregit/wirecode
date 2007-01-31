package com.limegroup.gnutella.altlocs;


import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * This class tests the methods of the <tt>AlternateLocation</tt> class.
 */
@SuppressWarnings("unchecked")
public final class AlternateLocationTest extends com.limegroup.gnutella.util.LimeTestCase {

    

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
			URN.createSHA1Urn(HugeTestUtils.VALID_URN_STRINGS[i]);
			URL url = new URL("http", HugeTestUtils.HOST_STRINGS[i], 6346, 
							  HTTPConstants.URI_RES_N2R+
							  HugeTestUtils.URNS[i].httpStringValue());
			HugeTestUtils.create(url);
		}
	}

	/**
	 * Tests the constructor that takes a string as an argument to make sure it
	 * succeeds when it should.
	 */
	public void testStringUrnConstructorForSuccess() throws Exception {
		for(int i=0; i<HugeTestUtils.URNS.length; i++) {
			URN.createSHA1Urn(HugeTestUtils.VALID_URN_STRINGS[i]);
			String url = "http://"+HugeTestUtils.HOST_STRINGS[i]+":6346"+ 
				HTTPConstants.URI_RES_N2R+
				HugeTestUtils.URNS[i].httpStringValue();
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
				HugeTestUtils.create(url);
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
                                   false,false,"",null, -1);

            // just make sure this doesn't throw an exception
			AlternateLocation loc = AlternateLocation.create(rfd);
			assertFalse(loc instanceof PushAltLoc);
		}


        RemoteFileDesc rfd = 
            new RemoteFileDesc("127.0.2.1", 6346, 10, HTTPConstants.URI_RES_N2R+
                                   HugeTestUtils.URNS[0].httpStringValue(), 10, 
                                   GUID.makeGuid(), 10, true, 2, true, null, 
                                   HugeTestUtils.URN_SETS[0],
                                   false,false,"",null, -1);

        AlternateLocation.create(rfd);

        try {
            AlternateLocation.create((RemoteFileDesc)null);
            fail("should have thrown a null pointer");
        } catch(NullPointerException e) {
            // this is expected
        }
        
        IpPort ppi = new IpPortImpl("1.2.3.4",6346);
		Set proxies = new IpPortSet();
		proxies.add(ppi);
		
        new PushEndpoint(GUID.makeGuid(),proxies);
        //test an rfd with push proxies
        RemoteFileDesc fwalled = new RemoteFileDesc("1.2.3.4",5,10,HTTPConstants.URI_RES_N2R+
                                   HugeTestUtils.URNS[0].httpStringValue(), 10, 
                                   GUID.makeGuid(), 10, true, 2, true, null, 
                                   HugeTestUtils.URN_SETS[0],
                                   false,true,"",proxies,-1);
        
        AlternateLocation loc = AlternateLocation.create(fwalled);
        
        assertTrue(loc instanceof PushAltLoc);
        PushAltLoc push = (PushAltLoc)loc;
        assertEquals("1.2.3.4",push.getPushAddress().getAddress());
        assertEquals(0, push.supportsFWTVersion());
        
        // test rfd with push proxies, external address that can do FWT
        RemoteFileDesc FWTed = new RemoteFileDesc("1.2.3.4",5,10,HTTPConstants.URI_RES_N2R+
                                   HugeTestUtils.URNS[0].httpStringValue(), 10, 
                                   GUID.makeGuid(), 10, true, 2, true, null, 
                                   HugeTestUtils.URN_SETS[0],
                                   false,true,"",proxies,-1,1);
        
        loc = AlternateLocation.create(FWTed);
        
        assertTrue(loc instanceof PushAltLoc);
        push = (PushAltLoc)loc;
        
        assertEquals("1.2.3.4",push.getPushAddress().getAddress());
        assertGreaterThan(0, push.supportsFWTVersion());
        assertEquals(5,push.getPushAddress().getPort());
        
        
	}

	/**
	 * Tests the factory method that creates a RemoteFileDesc from an alternate
	 * location.
	 */
	public void testCreateRemoteFileDesc() throws Exception{
		for(int i=0; i<HugeTestUtils.UNEQUAL_SHA1_LOCATIONS.length; i++) {
			DirectAltLoc al = (DirectAltLoc) HugeTestUtils.UNEQUAL_SHA1_LOCATIONS[i];
			RemoteFileDesc rfd = al.createRemoteFileDesc(10);
			assertEquals("SHA1s should be equal", al.getSHA1Urn(), rfd.getSHA1Urn());
			assertEquals("hosts should be equals",al.getHost().getAddress(),
					rfd.getHost());
			assertEquals("ports should be equals",al.getHost().getPort(),
					rfd.getPort());
		}
		
		IpPort ppi = new IpPortImpl("1.2.3.4",6346);
		Set proxies = new HashSet();
		proxies.add(ppi);
		
        new PushEndpoint(GUID.makeGuid(),proxies);
        //test an rfd with push proxies
        RemoteFileDesc fwalled = new RemoteFileDesc("1.2.3.4",5,10,HTTPConstants.URI_RES_N2R+
                                   HugeTestUtils.URNS[0].httpStringValue(), 10, 
                                   GUID.makeGuid(), 10, true, 2, true, null, 
                                   HugeTestUtils.URN_SETS[0],
                                   false,true,"",proxies,-1);
        
        AlternateLocation loc = AlternateLocation.create(fwalled);
        
        RemoteFileDesc other = loc.createRemoteFileDesc(3);
        assertEquals("1.2.3.4",other.getHost());
        assertEquals(5,other.getPort());
        
        assertEquals(fwalled.getClientGUID(),other.getClientGUID());
        assertEquals(fwalled.getPushAddr(),other.getPushAddr());
        
    }
	
	public void testCloningPushLocs() throws Exception {
	    IpPort ppi = new IpPortImpl("1.2.3.4",6346);
		Set proxies = new HashSet();
		proxies.add(ppi);
		
        new PushEndpoint(GUID.makeGuid(),proxies);
        //test an rfd with push proxies
        RemoteFileDesc fwalled = new RemoteFileDesc("127.0.0.1",6346,10,HTTPConstants.URI_RES_N2R+
                                   HugeTestUtils.URNS[0].httpStringValue(), 10, 
                                   GUID.makeGuid(), 10, true, 2, true, null, 
                                   HugeTestUtils.URN_SETS[0],
                                   false,true,"",proxies,-1);

        AlternateLocation loc = AlternateLocation.create(fwalled);
        
        assertTrue(loc instanceof PushAltLoc);
        
        AlternateLocation loc2 = loc.createClone();
        
        assertTrue(loc2 instanceof PushAltLoc);
        
        assertEquals(loc,loc2);
        Map m = (Map) PrivilegedAccessor.getValue(PushEndpoint.class,"GUID_PROXY_MAP");
        m.clear();
	}

	/**
	 * Tests the constructor that only takes a string argument for 
	 * valid alternate location strings that include timestamps.
	 */
	public void testStringConstructorForTimestampedLocs() throws Exception {
        try {
            for(int i=0; i<HugeTestUtils.VALID_TIMESTAMPED_LOCS.length; i++)
                AlternateLocation.create(HugeTestUtils.VALID_TIMESTAMPED_LOCS[i]);
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
				HugeTestUtils.create(HugeTestUtils.BAD_PORT_URLS[i]);
				fail("alternate location string should not have been accepted: "+
					 HugeTestUtils.BAD_PORT_URLS[i]);
			}			
		} catch(IOException e) {
			// this is what we're expecting
		}
	}
	
	/**
	 * Tests the location/urn constructor for success.
	 */
	public void testStringUrnConstructor() throws Exception {
	    ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
	    URN urn =
	        URN.createSHA1Urn("urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPTE");
	    
	    // First test with old-style locs.
		for(int i=0; i<HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length; i++) {
			AlternateLocation.create(
			    HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i], urn);
		}
		
		// Now make sure that the URN-mismatch works
		urn = URN.createSHA1Urn("urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPTD");
		for(int i=0; i<HugeTestUtils.VALID_NONTIMESTAMPED_LOCS.length; i++) {
			try {
				AlternateLocation.create(
				    HugeTestUtils.VALID_NONTIMESTAMPED_LOCS[i], urn);
                fail("IOException expected");
			} catch(IOException expected) {}
		}
		
		// Now try the new-style values
		for(int i = 1; i < 254; i++) {
	        String ip = i+"."+(i % 2)+"."+(i % 25)+"."+(i % 100);
	        DirectAltLoc al = (DirectAltLoc) AlternateLocation.create(ip + ":50", urn);
	        IpPort ep = al.getHost();
	        assertEquals(ip, ep.getAddress());
	        assertEquals(50, ep.getPort());
	        assertEquals(urn, al.getSHA1Urn());
        }
        
        // Try without a port.
		for(int i = 1; i < 254; i++) {
	        String ip = i+"."+(i % 2)+"."+(i % 25)+"."+(i % 100);
	        DirectAltLoc al = (DirectAltLoc)AlternateLocation.create(ip, urn);
	        IpPort ep = al.getHost();
	        assertEquals(ip, ep.getAddress());
	        assertEquals(6346, ep.getPort());
	        assertEquals(urn, al.getSHA1Urn());
        }
        
        // Try with bad values.
		for(int i = 1; i < 254; i++) {
		    try {
	            String ip = i+"."+(i % 2)+"."+(i % 25)+"."+(i % 100)+".1";
	            AlternateLocation.create(ip + ":50", urn);
                fail("IOException expected");
            } catch(IOException expected) {}
        }
        
        try {
            AlternateLocation.create("0.1.2.3", urn);
            fail("IOException expected");
        } catch(IOException expected) {}

        try {
            AlternateLocation.create("1.2.3.4/25", urn);
            fail("IOException expected");
        } catch(IOException expected) {}

        try {
            AlternateLocation.create("limewire.org", urn);
            fail("IOException expected");
        } catch(IOException expected) {}
        
        //try some firewalled locs
        GUID clientGUID = new GUID(GUID.makeGuid());
        String httpString=clientGUID.toHexString()+";1.2.3.4:15;1.2.3.5:16";
        
        PushAltLoc pal = (PushAltLoc)AlternateLocation.create(httpString,urn);
        
        assertTrue(Arrays.equals(
        		clientGUID.bytes(),pal.getPushAddress().getClientGUID()));
        assertEquals(2,pal.getPushAddress().getProxies().size());
        
        
        //try some valid push proxies, some invalid ones
        clientGUID = new GUID(GUID.makeGuid());
        httpString=clientGUID.toHexString()+";1.2.3.4:15;1.2.3.5:16";
        pal = (PushAltLoc) AlternateLocation.create(httpString+";0.1.2.3:100000;1.2.3.6:17",urn);
    	
        assertTrue(Arrays.equals(
        		clientGUID.bytes(),pal.getPushAddress().getClientGUID()));
        assertEquals(3,pal.getPushAddress().getProxies().size());
        
        //HashSets do not guarantee order so the resulting http string 
        //may contain the proxies in different order
        assertNotEquals(-1,pal.httpStringValue().indexOf(clientGUID.toHexString()));
        assertNotEquals(-1,pal.httpStringValue().indexOf("1.2.3.4:15"));
        assertNotEquals(-1,pal.httpStringValue().indexOf("1.2.3.5:16"));
        
        //try some valid push proxies and an empty one
        clientGUID = new GUID(GUID.makeGuid());
        httpString=clientGUID.toHexString()+";1.2.3.4:15;1.2.3.5:16";
        pal = (PushAltLoc) AlternateLocation.create(httpString+";;1.2.3.6:17",urn);
    	
        assertTrue(Arrays.equals(
        		clientGUID.bytes(),pal.getPushAddress().getClientGUID()));
        assertEquals(3,pal.getPushAddress().getProxies().size());
        
        
        //try an altloc with no push proxies
        clientGUID = new GUID(GUID.makeGuid());
      	pal = (PushAltLoc) AlternateLocation.create(clientGUID.toHexString()+";",urn);
      	
      	// try skipping invalid ip:port strings
      	pal = (PushAltLoc) AlternateLocation.create(
        		clientGUID.toHexString()+";"+ "1.2.3.4/:12",urn);
        assertTrue(pal.getPushAddress().getProxies().isEmpty());
        
        
        //try some invalid ones
        try {
        	pal = (PushAltLoc) AlternateLocation.create("asdf2345dgalshlh",urn);
        	fail("created altloc from garbage");
        }catch(IOException expected) {}
        
        try {
        	pal = (PushAltLoc) AlternateLocation.create("",urn);
        	fail("created altloc from empty string");
        }catch(IOException expected) {}
        
        try {
        	pal = (PushAltLoc) AlternateLocation.create(null,urn);
        	fail("created altloc from null string");
        }catch(IOException expected) {}
        
    }

    public void testDemotedEquals() throws Exception {
        AlternateLocation loc1 = AlternateLocation.create(equalLocs[0]);
        AlternateLocation loc2 = AlternateLocation.create(equalLocs[0]);
        assertEquals("locations should be equal", loc1, loc2);
        loc2.demote();
        assertEquals("locations should be equal", loc1, loc2);
    }
    
    
    public void testCompareTo() throws Exception {
        TreeSet set = new TreeSet();
        
        AlternateLocation direct1 = AlternateLocation.create(equalLocs[0]);
        AlternateLocation direct2 = AlternateLocation.create(equalLocs[0]);
        
        set.add(direct1);
        assertTrue(set.contains(direct2));
        
        direct2.increment();
        assertFalse(set.contains(direct2));
        assertLessThan(0,direct1.compareTo(direct2));
        
        set.remove(direct1);
        direct1.demote();
        assertGreaterThan(0,direct1.compareTo(direct2));
        
        direct1.promote();
        assertLessThan(0,direct1.compareTo(direct2));
        
        // try some push altlocs
        GUID clientGUID = new GUID(GUID.makeGuid());
        String httpString=clientGUID.toHexString()+";1.2.3.4:15;1.2.3.5:16";
        
        URN urn =
	        URN.createSHA1Urn("urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPTE");
        
        AlternateLocation push1 = AlternateLocation.create(httpString,urn);
        AlternateLocation push2 = AlternateLocation.create(httpString,urn);
        
        assertTrue(push1.equals(push2));
        
        assertEquals(0,push1.compareTo(push2));
        
        push2.increment();
        assertLessThan(0,push1.compareTo(push2));
        
        // calling demote() does not affect push locs
        push1.demote();
        assertLessThan(0,push1.compareTo(push2));
        
        // comparing the two types of altlocs is predictable if their count
        // values are different
        assertGreaterThan(0,direct2.compareTo(push1));
        
        // it is not easily predictable if they are the same
        assertNotEquals(0,direct2.compareTo(push2));
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
					AlternateLocation.create(loc);
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
                    AlternateLocation.create(loc);
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
                AlternateLocation.create(loc);
                fail("alt loc should not have accepted firewalled loc: "+loc);
            } catch(Exception e) {
                // this is expected 
            }
        }

        for(int i=0; i<HugeTestUtils.NON_FIREWALLED_LOCS.length; i++) {
            String loc = HugeTestUtils.NON_FIREWALLED_LOCS[i];
            AlternateLocation.create(loc);
        }
    }
    
}
