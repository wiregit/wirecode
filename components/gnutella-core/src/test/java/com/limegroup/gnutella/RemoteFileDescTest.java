package com.limegroup.gnutella;


import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;

import com.google.inject.Injector;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.http.HTTPConstants;

/**
 * This class tests the methods of the <tt>RemoteFileDesc</tt> class.
 */
@SuppressWarnings("unchecked")
public final class RemoteFileDescTest extends com.limegroup.gnutella.util.LimeTestCase {	  

	private byte[] TEST_GUID;
    private PushEndpointFactory pushEndpointFactory;
    private RemoteFileDescFactory remoteFileDescFactory;

	public RemoteFileDescTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(RemoteFileDescTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	@Override
    protected void setUp() {
	    Injector injector = LimeTestUtils.createInjector();
	    pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
	    remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
		TEST_GUID = GUID.makeGuid();
	}

	/**
	 * Make sure that invalid port values are rejected.
	 */
	public void testConstructorWithInvalidPorts() {
		int[] invalidPorts = new int[2];
		invalidPorts[0] = -1;
		invalidPorts[1] = 66000;
		for(int i=0; i<invalidPorts.length; i++) {
			try {
				RemoteFileDesc rfd = 
				    remoteFileDescFactory.createRemoteFileDesc("www.limewire.org", invalidPorts[i], 10, "test", 10,
                        TEST_GUID, 10, false, 3, false, null, null, false, false, "", null, -1,
                        false);
				fail("rfd1 should have received an exception for invalid port: "+
					 rfd.getPort());
			} catch(IllegalArgumentException e) {
				// this is expected
			}
		}
	}

	/**
	 * Make sure that valid port values are accepted.
	 */
	public void testConstructorWithValidPorts() {
		int[] validPorts = new int[2];
		validPorts[0] = 10000;
		validPorts[1] = 6000;
		for(int i=0; i<validPorts.length; i++) {
			try {
			    remoteFileDescFactory.createRemoteFileDesc("www.limewire.org", validPorts[i], 10, "test", 10,
                        TEST_GUID, 10, false, 3, false, null, null, false, false, "", null, -1,
                        false);
			} catch(IllegalArgumentException e) {
				fail("rfd1 should not have received an exception for valid port: "+
					 validPorts[i], e);
			}
		}
	}

	/**
	 * Tests the getUrl method to make sure it's correctly constructing URLs.
	 */
	public void testRemoteFileDescGetUrl() {
		Set urns = new HashSet();
		urns.add(UrnHelper.URNS[0]);
		RemoteFileDesc rfd =
		    remoteFileDescFactory.createRemoteFileDesc("www.test.org", 3000, 10, "test", 10, TEST_GUID, 10, true, 3,
                true, null, urns, false, false, "", null, -1, false);
		URL rfdUrl = rfd.getUrl();
		String urlString = rfdUrl.toString();
		String host = rfd.getHost();
		String colonPort = ":"+rfd.getPort();
		assertTrue("unexpected beginning of url", 
				   urlString.startsWith("http://"+host+colonPort));
		assertEquals("unexpected double slash",
		    urlString.indexOf(colonPort+"//"), -1);
		assertNotEquals("unexpected double slash",
		    -1, urlString.indexOf(":3000/"));
	}
	
	/**
	 * tests if the rfd correctly determines if it is altloc and push capable
	 */
	public void testIsAltlocPushCapable() throws Exception {
		
		IpPort ppi = new IpPortImpl("1.2.3.4",6346);
		IpPort ppi2 = new IpPortImpl("1.2.3.4",6346);
		Set proxies = new IpPortSet();
		Set proxies2 = new IpPortSet();
		proxies.add(ppi);
		proxies2.add(ppi);
		proxies2.add(ppi2);
		
		GUID g1 = new GUID(GUID.makeGuid());
		GUID g2 = new GUID(GUID.makeGuid());
        PushEndpoint pe = pushEndpointFactory.createPushEndpoint(g1.bytes(), proxies);
        PushEndpoint pe2 = pushEndpointFactory.createPushEndpoint(g2.bytes(), proxies2);
        
        //test an rfd with push proxies
		 RemoteFileDesc fwalled = remoteFileDescFactory.createRemoteFileDesc("127.0.0.1", 6346, 10, HTTPConstants.URI_RES_N2R+
                UrnHelper.URNS[0].httpStringValue(), 10,
                pe.getClientGUID(), 10, true, 2, true, null, UrnHelper.URN_SETS[0], false, true, "", proxies,
                -1, false);
		 
		 assertTrue(Arrays.equals(pe.getClientGUID(),fwalled.getClientGUID()));
		 
		 RemoteFileDesc nonfwalled = 
		     remoteFileDescFactory.createRemoteFileDesc("www.limewire.org", 6346, 10, HTTPConstants.URI_RES_N2R+
        				   UrnHelper.URNS[1].httpStringValue(), 10,
                GUID.makeGuid(), 10, true, 2, true, null, UrnHelper.URN_SETS[1], false, false, "", null,
                -1, false);
		 
		 RemoteFileDesc differentPE = remoteFileDescFactory.createRemoteFileDesc(fwalled, pe2);
		 assertTrue(Arrays.equals(pe2.getClientGUID(),differentPE.getClientGUID()));
		 
		 //both rfds should report as being altloc capable, but only
		 //the firewalled rfd should be pushCapable
		 assertTrue(fwalled.isAltLocCapable());
		 assertTrue(fwalled.needsPush());
		 assertTrue(nonfwalled.isAltLocCapable());
		 assertFalse(nonfwalled.needsPush());
		 
		 //now create an rfd which claims to be firewalled but has no push proxies
		 GUID g3 = new GUID(GUID.makeGuid());
		 PushEndpoint noProxies = pushEndpointFactory.createPushEndpoint(g3.bytes());

		 RemoteFileDesc fwalledNotGood = 
		     remoteFileDescFactory.createRemoteFileDesc(fwalled, noProxies);
		 
		 //it should not be a capable altloc.
		 assertFalse(fwalledNotGood.isAltLocCapable());
		 assertTrue(fwalledNotGood.needsPush());
	}
	
}
