package com.limegroup.gnutella.altlocs;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Test;

import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.helpers.AlternateLocationHelper;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * This class tests the methods of the <tt>AlternateLocation</tt> class.
 */
@SuppressWarnings("unchecked")
public final class AlternateLocationTest extends com.limegroup.gnutella.util.LimeTestCase {

    /**
     * Alternate locations without timestamps that are not firewalled.
     */
    public static final String[] NON_FIREWALLED_LOCS = {
        "50.40.39.40:6352",
        "51.20.12.36:6352",
        "52.47.12.36:6352",
        "53.40.201.35:6322",
        "201.24.40.67:6352",
        "201.28.40.24:6352"
    };
    
    public static final String[] FIREWALLED_LOCS = {
        "192.168.39.40:6352",
        "127.20.12.36:6352",
        "10.47.12.36:6352",
        "172.16.201.35:6322",
        "172.17.12.36:6332",
        "172.18.40.67:6352",
        "172.31.40.24:6352",
    };
    
    private AlternateLocationHelper alternateLocationHelper;

    private static String HASH = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";

	private static final String[] equalLocs = {
		"200.30.1.02:6352", "200.30.1.02:6352"
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
        alternateLocationHelper = new AlternateLocationHelper(ProviderHacks.getAlternateLocationFactory());
    }

	/**
	 * Tests the constructor that creates an alternate location from a remote
	 * file desc.
	 */
	public void testRemoteFileDescConstructor() throws Exception {
		for(int i=0; i<UrnHelper.URNS.length; i++) {
			RemoteFileDesc rfd = 
				new RemoteFileDesc("www.limewire.org", 6346, 10, HTTPConstants.URI_RES_N2R+
								   UrnHelper.URNS[i].httpStringValue(), 10, 
								   GUID.makeGuid(), 10, true, 2, true, null, 
								   UrnHelper.URN_SETS[i],
                                   false,false,"",null, -1, false);

            // just make sure this doesn't throw an exception
			AlternateLocation loc = ProviderHacks.getAlternateLocationFactory().create(rfd);
			assertFalse(loc instanceof PushAltLoc);
		}


        RemoteFileDesc rfd = 
            new RemoteFileDesc("127.0.2.1", 6346, 10, HTTPConstants.URI_RES_N2R+
                                   UrnHelper.URNS[0].httpStringValue(), 10, 
                                   GUID.makeGuid(), 10, true, 2, true, null, 
                                   UrnHelper.URN_SETS[0],
                                   false,false,"",null, -1, false);

        ProviderHacks.getAlternateLocationFactory().create(rfd);

        try {
            ProviderHacks.getAlternateLocationFactory().create((RemoteFileDesc)null);
            fail("should have thrown a null pointer");
        } catch(NullPointerException e) {
            // this is expected
        }
        
        IpPort ppi = new IpPortImpl("1.2.3.4",6346);
		Set proxies = new IpPortSet();
		proxies.add(ppi);
		
        ProviderHacks.getPushEndpointFactory().createPushEndpoint(GUID.makeGuid(), proxies);
        //test an rfd with push proxies
        RemoteFileDesc fwalled = new RemoteFileDesc("1.2.3.4",5,10,HTTPConstants.URI_RES_N2R+
                                   UrnHelper.URNS[0].httpStringValue(), 10, 
                                   GUID.makeGuid(), 10, true, 2, true, null, 
                                   UrnHelper.URN_SETS[0],
                                   false,true,"",proxies,-1, false);
        
        AlternateLocation loc = ProviderHacks.getAlternateLocationFactory().create(fwalled);
        
        assertTrue(loc instanceof PushAltLoc);
        PushAltLoc push = (PushAltLoc)loc;
        assertEquals("1.2.3.4",push.getPushAddress().getAddress());
        assertEquals(0, push.supportsFWTVersion());
        
        // test rfd with push proxies, external address that can do FWT
        RemoteFileDesc FWTed = new RemoteFileDesc("1.2.3.4",5,10,HTTPConstants.URI_RES_N2R+
                                   UrnHelper.URNS[0].httpStringValue(), 10, 
                                   GUID.makeGuid(), 10, true, 2, true, null, 
                                   UrnHelper.URN_SETS[0],
                                   false,true,"",proxies,-1,1, false);
        
        loc = ProviderHacks.getAlternateLocationFactory().create(FWTed);
        
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
		for(int i=0; i<alternateLocationHelper.UNEQUAL_SHA1_LOCATIONS.length; i++) {
			DirectAltLoc al = (DirectAltLoc) alternateLocationHelper.UNEQUAL_SHA1_LOCATIONS[i];
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
		
        ProviderHacks.getPushEndpointFactory().createPushEndpoint(GUID.makeGuid(), proxies);
        //test an rfd with push proxies
        RemoteFileDesc fwalled = new RemoteFileDesc("1.2.3.4",5,10,HTTPConstants.URI_RES_N2R+
                                   UrnHelper.URNS[0].httpStringValue(), 10, 
                                   GUID.makeGuid(), 10, true, 2, true, null, 
                                   UrnHelper.URN_SETS[0],
                                   false,true,"",proxies,-1, false);
        
        AlternateLocation loc = ProviderHacks.getAlternateLocationFactory().create(fwalled);
        
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
		
        ProviderHacks.getPushEndpointFactory().createPushEndpoint(GUID.makeGuid(), proxies);
        //test an rfd with push proxies
        RemoteFileDesc fwalled = new RemoteFileDesc("127.0.0.1",6346,10,HTTPConstants.URI_RES_N2R+
                                   UrnHelper.URNS[0].httpStringValue(), 10, 
                                   GUID.makeGuid(), 10, true, 2, true, null, 
                                   UrnHelper.URN_SETS[0],
                                   false,true,"",proxies,-1, false);

        AlternateLocation loc = ProviderHacks.getAlternateLocationFactory().create(fwalled);
        
        assertTrue(loc instanceof PushAltLoc);
        
        AlternateLocation loc2 = loc.createClone();
        
        assertTrue(loc2 instanceof PushAltLoc);
        
        assertEquals(loc,loc2);
        Map m = (Map) PrivilegedAccessor.getValue(PushEndpoint.class,"GUID_PROXY_MAP");
        m.clear();
	}
	
	/**
	 * Tests the location/urn constructor for success.
	 */
	public void testStringUrnConstructor() throws Exception {
	    ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
	    URN urn = URN.createSHA1Urn("urn:sha1:ULSTTIPQGSSZTS5FJUPAKUZWUGYQYPTE");
	    
		// Now try the new-style values
		for(int i = 1; i < 254; i++) {
	        String ip = i+"."+(i % 2)+"."+(i % 25)+"."+(i % 100);
	        DirectAltLoc al = (DirectAltLoc) ProviderHacks.getAlternateLocationFactory().create(ip + ":50", urn);
	        IpPort ep = al.getHost();
	        assertEquals(ip, ep.getAddress());
	        assertEquals(50, ep.getPort());
	        assertEquals(urn, al.getSHA1Urn());
        }
        
        // Try without a port.
		for(int i = 1; i < 254; i++) {
	        String ip = i+"."+(i % 2)+"."+(i % 25)+"."+(i % 100);
	        DirectAltLoc al = (DirectAltLoc)ProviderHacks.getAlternateLocationFactory().create(ip, urn);
	        IpPort ep = al.getHost();
	        assertEquals(ip, ep.getAddress());
	        assertEquals(6346, ep.getPort());
	        assertEquals(urn, al.getSHA1Urn());
        }
        
        // Try with bad values.
		for(int i = 1; i < 254; i++) {
		    try {
	            String ip = i+"."+(i % 2)+"."+(i % 25)+"."+(i % 100)+".1";
	            ProviderHacks.getAlternateLocationFactory().create(ip + ":50", urn);
                fail("IOException expected");
            } catch(IOException expected) {}
        }
        
        try {
            ProviderHacks.getAlternateLocationFactory().create("0.1.2.3", urn);
            fail("IOException expected");
        } catch(IOException expected) {}

        try {
            ProviderHacks.getAlternateLocationFactory().create("1.2.3.4/25", urn);
            fail("IOException expected");
        } catch(IOException expected) {}

        try {
            ProviderHacks.getAlternateLocationFactory().create("limewire.org", urn);
            fail("IOException expected");
        } catch(IOException expected) {}
        
        //try some firewalled locs
        GUID clientGUID = new GUID(GUID.makeGuid());
        String httpString=clientGUID.toHexString()+";1.2.3.4:15;1.2.3.5:16";
        
        PushAltLoc pal = (PushAltLoc)ProviderHacks.getAlternateLocationFactory().create(httpString,urn);
        
        assertTrue(Arrays.equals(
        		clientGUID.bytes(),pal.getPushAddress().getClientGUID()));
        assertEquals(2,pal.getPushAddress().getProxies().size());
        
        
        //try some valid push proxies, some invalid ones
        clientGUID = new GUID(GUID.makeGuid());
        httpString=clientGUID.toHexString()+";1.2.3.4:15;1.2.3.5:16";
        pal = (PushAltLoc) ProviderHacks.getAlternateLocationFactory().create(httpString+";0.1.2.3:100000;1.2.3.6:17",urn);
    	
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
        pal = (PushAltLoc) ProviderHacks.getAlternateLocationFactory().create(httpString+";;1.2.3.6:17",urn);
    	
        assertTrue(Arrays.equals(
        		clientGUID.bytes(),pal.getPushAddress().getClientGUID()));
        assertEquals(3,pal.getPushAddress().getProxies().size());
        
        
        //try an altloc with no push proxies
        clientGUID = new GUID(GUID.makeGuid());
      	pal = (PushAltLoc) ProviderHacks.getAlternateLocationFactory().create(clientGUID.toHexString()+";",urn);
      	
      	// try skipping invalid ip:port strings
      	pal = (PushAltLoc) ProviderHacks.getAlternateLocationFactory().create(
        		clientGUID.toHexString()+";"+ "1.2.3.4/:12",urn);
        assertTrue(pal.getPushAddress().getProxies().isEmpty());
        
        
        //try some invalid ones
        try {
        	pal = (PushAltLoc) ProviderHacks.getAlternateLocationFactory().create("asdf2345dgalshlh",urn);
        	fail("created altloc from garbage");
        }catch(IOException expected) {}
        
        try {
        	pal = (PushAltLoc) ProviderHacks.getAlternateLocationFactory().create("",urn);
        	fail("created altloc from empty string");
        }catch(IOException expected) {}
        
        try {
        	pal = (PushAltLoc) ProviderHacks.getAlternateLocationFactory().create(null,urn);
        	fail("created altloc from null string");
        }catch(IOException expected) {}
        
    }

    public void testDemotedEquals() throws Exception {
        AlternateLocation loc1 = ProviderHacks.getAlternateLocationFactory().create(equalLocs[0], URN.createSHA1Urn(HASH));
        AlternateLocation loc2 = ProviderHacks.getAlternateLocationFactory().create(equalLocs[0], URN.createSHA1Urn(HASH));
        assertEquals("locations should be equal", loc1, loc2);
        loc2.demote();
        assertEquals("locations should be equal", loc1, loc2);
    }
    
    
    public void testCompareTo() throws Exception {
        TreeSet set = new TreeSet();
        
        AlternateLocation direct1 = ProviderHacks.getAlternateLocationFactory().create(equalLocs[0], URN.createSHA1Urn(HASH));
        AlternateLocation direct2 = ProviderHacks.getAlternateLocationFactory().create(equalLocs[0], URN.createSHA1Urn(HASH));
        
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
        
        AlternateLocation push1 = ProviderHacks.getAlternateLocationFactory().create(httpString,urn);
        AlternateLocation push2 = ProviderHacks.getAlternateLocationFactory().create(httpString,urn);
        
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
			AlternateLocation curLoc = ProviderHacks.getAlternateLocationFactory().create(equalLocs[i], URN.createSHA1Urn(HASH));
			for(int j=0; j<equalLocs.length; j++) {
				AlternateLocation newLoc=ProviderHacks.getAlternateLocationFactory().create(equalLocs[j], URN.createSHA1Urn(HASH));
				assertEquals("locations should be equal", curLoc, newLoc);
			}
		}
	}

	/**
	 * Tests the compareTo method of the AlternateLocation class.
	 */
	public void testAlternateLocationCompareTo() throws Exception {
		for(int i=0; i<equalLocs.length; i++) {
			AlternateLocation curLoc = 
			    ProviderHacks.getAlternateLocationFactory().create(equalLocs[i], URN.createSHA1Urn(HASH));
			for(int j=0; j<equalLocs.length; j++) {
				AlternateLocation newLoc = 
				    ProviderHacks.getAlternateLocationFactory().create(equalLocs[j], URN.createSHA1Urn(HASH));
				int z = curLoc.compareTo(newLoc);
				assertEquals("locations should be equal", 0, z);
			}
		}
	}

    /**
     * Test to make sure that we're handling firewalls fine -- rejecting
     * firewalled locations and accepting non-firewalled locations.
     */
    public void testAlternateLocationToMakeSureItDisallowsFirewalledHosts() throws Exception {
        for(int i=0; i<AlternateLocationTest.FIREWALLED_LOCS.length; i++) {
            String loc = AlternateLocationTest.FIREWALLED_LOCS[i];
            try {
                ProviderHacks.getAlternateLocationFactory().create(loc, UrnHelper.URNS[0]);
                fail("alt loc should not have accepted firewalled loc: "+loc);
            } catch(IOException e) {
                // this is expected 
            }
        }

        for(int i=0; i<alternateLocationHelper.SOME_IPS.length; i++) {
            String loc = alternateLocationHelper.SOME_IPS[i];
            ProviderHacks.getAlternateLocationFactory().create(loc, UrnHelper.URNS[0]);
        }
    }
    
    public void testAlternateLocationKeepsTLSInfo() throws Exception {
        for(int i = 0; i < alternateLocationHelper.SOME_IPS.length; i++) {
            AlternateLocation nonTLS = ProviderHacks.getAlternateLocationFactory().create(alternateLocationHelper.SOME_IPS[i], UrnHelper.SHA1, false);
            AlternateLocation tls = ProviderHacks.getAlternateLocationFactory().create(alternateLocationHelper.SOME_IPS[i], UrnHelper.SHA1, true);
            
            DirectAltLoc d1 = (DirectAltLoc)nonTLS;
            DirectAltLoc d2 = (DirectAltLoc)tls;
            
            IpPort p1 = d1.getHost();
            IpPort p2 = d2.getHost();
            
            if(p1 instanceof Connectable)
                assertFalse(((Connectable)p1).isTLSCapable());
            assertInstanceof(Connectable.class, p2);
            assertTrue(((Connectable)p2).isTLSCapable());
        }
    }
    
}
