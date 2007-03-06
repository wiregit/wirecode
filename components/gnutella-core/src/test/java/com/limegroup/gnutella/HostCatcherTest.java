package com.limegroup.gnutella;

import java.io.File;
import java.io.FileWriter;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import java.util.zip.GZIPOutputStream;

import junit.framework.Test;


import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FixedsizePriorityQueue;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.bootstrap.UDPHostCache;
import com.limegroup.gnutella.bootstrap.BootstrapServerManager;


public class HostCatcherTest extends BaseTestCase {  
    private HostCatcher hc;

    public HostCatcherTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HostCatcherTest.class);
    }


    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Returns a new HostCatcher connected to stubs. YOU MAY WANT TO CALL EXPIRE to force bootstrap pongs.
     */
    public void setUp() {
        // explicitly allow all ips to test.
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[] {});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] { "*.*" });

        HostCatcher.DEBUG = true;
        new RouterService(new ActivityCallbackStub());

        hc = new HostCatcher();
        hc.initialize();		
    }

    /**
     * Test the method for putting hosts on probation.
     * 
     * @throws Exception if an error occurs
     */
    public void testPutHostOnProbation() throws Exception {
        HostCatcher catcher = new HostCatcher();
        String ipStart = "34.56.";
        int penultimatetByte;
        for(int i=0; i<HostCatcher.PROBATION_HOSTS_SIZE; i++) {
            
            // Add a bunch of unique endpoints.
            if(i >= 512) {
                penultimatetByte = 2;
            } else if(i >= 255) {
                penultimatetByte = 1;
            } else {
                penultimatetByte = 0;
            }
            
            int lastByte = i%256;
            Endpoint curHost = 
                new Endpoint(ipStart+penultimatetByte+"."+lastByte, 6346);
            catcher.putHostOnProbation(curHost);
        }
        
        Set probatedHosts =
            (Set)PrivilegedAccessor.getValue(catcher, "PROBATION_HOSTS");
        
        assertEquals("unexpected size", HostCatcher.PROBATION_HOSTS_SIZE,
            probatedHosts.size());
        
        // Start adding slightly different IPs
        ipStart = "35.56.5.";
        for(int i=0; i<10; i++) {
            Endpoint curHost = new Endpoint(ipStart+i, 6346);
            catcher.putHostOnProbation(curHost);
            assertEquals("unexpected size", HostCatcher.PROBATION_HOSTS_SIZE,
            probatedHosts.size());
        }
    }
    
    
    /**
     * Test the method for expiring hosts
     * 
     * @throws Exception if an error occurs
     */
    public void testExpireHosts() throws Exception {
        HostCatcher catcher = new HostCatcher();
        String ipStart = "34.56.";
        int penultimatetByte;
        for(int i=0; i<HostCatcher.EXPIRED_HOSTS_SIZE; i++) {
            
            // Add a bunch of unique endpoints.
            if(i >= 512) {
                penultimatetByte = 2;
            } else if(i >= 255) {
                penultimatetByte = 1;
            } else {
                penultimatetByte = 0;
            }
            
            int lastByte = i%256;
            Endpoint curHost = 
                new Endpoint(ipStart+penultimatetByte+"."+lastByte, 6346);
            catcher.expireHost(curHost);
        }
        
        Set expiredHosts =
            (Set)PrivilegedAccessor.getValue(catcher, "EXPIRED_HOSTS");
        
        assertEquals("unexpected size", HostCatcher.EXPIRED_HOSTS_SIZE,
            expiredHosts.size());
        
        // Start adding slightly different IPs
        ipStart = "35.56.5.";
        for(int i=0; i<10; i++) {
            Endpoint curHost = new Endpoint(ipStart+i, 6346);
            catcher.putHostOnProbation(curHost);
            assertEquals("unexpected size", HostCatcher.EXPIRED_HOSTS_SIZE,
            expiredHosts.size());
        }
    }
    
    
    /**
     * Test to make sure we hit the GWebCache if hosts fail.
     */
    public void testHitsGWebCacheIfHostsFail() throws Exception {
        HostCatcher.DEBUG = false;
        PrivilegedAccessor.setValue(RouterService.class, "catcher", hc);
        StubGWebBootstrapper stub = new StubGWebBootstrapper();
        PrivilegedAccessor.setValue(hc, "gWebCache", stub);
        
        String startAddress = "30.4.5.";
        for(int i=0; i<250; i++) {
            Endpoint curHost = new Endpoint(startAddress+i, 6346);
            hc.add(curHost, true);
        }
        
 
        for(int i=0; i<250; i++) {
            Endpoint host = hc.getAnEndpoint();
            assertTrue("unexpected address", 
                host.getAddress().startsWith(startAddress));
        }
        
        
        for(int i=0; i<250; i++) {
            Endpoint curHost = new Endpoint(startAddress+i, 6346);
            hc.doneWithConnect(curHost, false);
        }
        
        assertFalse(stub.fetched);
        Endpoint gWebCacheHost = hc.getAnEndpoint();
        assertTrue(stub.fetched);
        assertEquals(stub.host, gWebCacheHost.getAddress());
        
        HostCatcher.DEBUG = true;
    }
    
    /**
     * Tests to make sure that the UDP Host Cache is used before 
     * GWebCaches are used, if we know of any host caches.
     */
    public void testUDPCachesUsedBeforeGWebCaches() throws Exception {
        assertEquals(0, hc.getNumHosts());
        PrivilegedAccessor.setValue(RouterService.class, "catcher", hc);        
        
        StubGWebBootstrapper gw = new StubGWebBootstrapper();
        StubUDPBootstrapper udp = new StubUDPBootstrapper();
        PrivilegedAccessor.setValue(hc, "gWebCache", gw);
        PrivilegedAccessor.setValue(hc, "udpHostCache", udp);
        
        Endpoint firstHost = hc.getAnEndpoint();
        assertTrue(udp.fetched);
        assertFalse(gw.fetched);
        assertEquals(udp.host, firstHost.getAddress());
        udp.fetched = false;
        
        // Since udp was done quickly and only gave us one host (and we
        // just used it), the next request will spark a GW request.
        long timeBefore = System.currentTimeMillis();
        Endpoint secondHost = hc.getAnEndpoint();
        long timeAfter = System.currentTimeMillis();
        assertFalse(udp.fetched);
        assertTrue(gw.fetched);
        assertEquals(gw.host, secondHost.getAddress());
        assertGreaterThan(15 * 1000, timeAfter - timeBefore);
        gw.fetched = false;
        udp.expired = false;
        
        // Now another fetch will wait until time passes enough to retry
        // udp (too long before retrying a GW)
        Endpoint thirdHost = hc.getAnEndpoint();
        assertTrue(udp.fetched);
        assertFalse(gw.fetched);
        assertEquals(udp.host, thirdHost.getAddress());
    }
        
    
    /**
     * Tests to make sure that we ignore hosts that have expired.
     * 
     * @throws Exception if any error occurs
     */
    public void testIgnoreExpiredHosts() throws Exception {
        Endpoint expiredHost = new Endpoint("20.4.5.7", 6346);
        HostCatcher catcher = new HostCatcher();
        catcher.initialize();
        catcher.add(expiredHost,true);
        assertEquals("unexpected number of hosts", 1, catcher.getNumHosts());
        Endpoint accessedHost = catcher.getAnEndpoint();
        assertNotNull("host should not be null", accessedHost);
        assertEquals("unexpected number of hosts", 0, catcher.getNumHosts());
        
        catcher.expireHost(expiredHost);
        catcher.add(expiredHost, true);
        assertEquals("unexpected number of hosts", 0, catcher.getNumHosts());        
    }
    
    /**
     * Tests to make sure that we ignore hosts that have been put on probation.
     * 
     * @throws Exception if any error occurs
     */
    public void testIgnoreProbatedHosts() throws Exception {
        Endpoint probatedHost = new Endpoint("20.4.5.7", 6346);
        HostCatcher catcher = new HostCatcher();
        catcher.initialize();
        catcher.add(probatedHost,true);
        assertEquals("unexpected number of hosts", 1, catcher.getNumHosts());
        Endpoint accessedHost = catcher.getAnEndpoint();
        assertNotNull("host should not be null", accessedHost);
        assertEquals("unexpected number of hosts", 0, catcher.getNumHosts());
        
        catcher.putHostOnProbation(probatedHost);
        catcher.add(probatedHost, true);
        assertEquals("unexpected number of hosts", 0, catcher.getNumHosts());        
    }
    
    /**
     * Tests to make sure that hosts that are put on probation are properly 
     * recovered.
     * 
     * @throws Exception if any error occurs
     */
    public void testRecoveryOfHostsOnProbation() throws Exception {
        HostCatcher catcher = new HostCatcher();
        long waitTime = 100;
        PrivilegedAccessor.setValue(HostCatcher.class, 
            "PROBATION_RECOVERY_WAIT_TIME", new Long(waitTime));
        long interval = 20000;
        PrivilegedAccessor.setValue(HostCatcher.class, 
            "PROBATION_RECOVERY_TIME", new Long(interval));
        
        Endpoint probatedHost = new Endpoint("20.4.5.7", 6346);
        
        // Put the host on probation.
        catcher.putHostOnProbation(probatedHost);
        
        catcher.add(probatedHost, true);
        
        // And make sure that it did not get added
        assertEquals("unexpected number of hosts", 0, catcher.getNumHosts());
        
        // Start the probation recovery sequence...
        catcher.initialize();        
        
        // Sleep until the recovery operation takes place...
        Thread.sleep(waitTime+200);
        
        // Finally, make sure we are then able to add the host that was 
        // formerly on probation.
        catcher.add(probatedHost, true);
        assertEquals("unexpected number of hosts", 1, catcher.getNumHosts());        
    }
    
    /**
     * Tests to make sure that recovering used hosts works as expected.  This
     * method is used when the user's network connection goes down.
     *
     * @throws Exception if any error occurs
     */    
    public void testRecoversUsedHosts() throws Exception {
        // write data to gnutella.net
        hc.add(new Endpoint("18.239.0.1"), false);
        hc.add(new Endpoint("18.239.0.2"), false);
        hc.add(new Endpoint("18.239.0.3"), false);
        hc.add(new Endpoint("18.239.0.4"), false);
        hc.add(new Endpoint("18.239.0.5"), false);
        hc.add(new Endpoint("18.239.0.6"), false);
        hc.add(new Endpoint("18.239.0.7"), false);
        hc.add(new Endpoint("18.239.0.8"), false);
        hc.add(new Endpoint("18.239.0.9"), false);
        hc.add(new Endpoint("18.239.0.10"), false);

        hc.write();
        int numHosts = hc.getNumHosts();

        for(int i=0; i<10; i++) {
            hc.getAnEndpoint();
        }
        
        assertEquals("hosts should be 0", 0, hc.getNumHosts());
        hc.recoverHosts();
        assertEquals("hosts should have been recovered", 
            numHosts, hc.getNumHosts());
    }
    
    /** Tests that FixedsizePriorityQueue can hold two endpoints with same
     *  priority but different ip's.  This was a problem at one point. */
    public void testEndpointPriorities() {
        Endpoint e1=new ExtendedEndpoint("18.239.0.146", 6346);
        Endpoint e2=new ExtendedEndpoint("18.239.0.147", 6347);
        assertNotEquals("e1 vs e2", e1, e2);
        assertNotEquals("e2 vs e1", e2, e1);
        
        FixedsizePriorityQueue queue=new FixedsizePriorityQueue(
            ExtendedEndpoint.priorityComparator(),
            10);
        assertNull(queue.insert(e1));
        assertNull(queue.insert(e2));
        assertEquals(2, queue.size());
    }


    public void testAddPriorities() {
        
        // HostCatcher should ignore attempts to add hosts with private 
        // addresses.
        hc.add(new Endpoint("192.168.0.1"), false);
        assertEquals("private endpoint added as ultrapeer",
					 0, hc.getNumUltrapeerHosts());

        assertEquals("private endpoint added at all",
					 0, hc.getNumHosts());

        setUp();
        // Adding a normal host should add 1 more to numNormalHosts
        hc.add(new Endpoint("18.239.0.1"), false);
        assertEquals("normal endpoint added as ultrapeer",
                0, hc.getNumUltrapeerHosts());

        setUp();
        // Adding a ultrapeer should add 1 more to numUltrapeerHosts
        hc.add(new Endpoint("18.239.0.1"), true);
        assertEquals("ultrapeer endpoint not added as ultrapeer",
                1, hc.getNumUltrapeerHosts());

        //PingReply's.
        setUp();
        // Adding a private should add 1 more to numPrivateHosts
        hc.add(PingReply.createExternal(PingRequest.UDP_GUID.bytes(), (byte)3, 6346, 
            new byte[] {(byte)192,(byte)168,(byte)0,(byte)1}, false));
        assertEquals("private PingReply added as ultrapeer",
					 0 ,hc.getNumUltrapeerHosts());

        setUp();
        hc.add(PingReply.createExternal(PingRequest.UDP_GUID.bytes(), (byte)3, 6346, 
            new byte[] {(byte)18,(byte)239,(byte)0,(byte)1}, false));
        assertEquals("normal PingReply added as ultrapeer",
                0, hc.getNumUltrapeerHosts());


        setUp();
        hc.add(PingReply.createExternal(PingRequest.UDP_GUID.bytes(), (byte)3, 6346, 
            new byte[] {(byte)18,(byte)239,(byte)0,(byte)1}, true));
        assertEquals("ultrapeer PingReply not added as ultrapeer",
                1, hc.getNumUltrapeerHosts());
    }


    public void testPermanent() throws Exception {
        //Systm.out.println("-Testing write of permanent nodes to Gnutella.net");
        //1. Create HC, add entries, write to disk.
        hc.add(new Endpoint("18.239.0.141", 6341), false);//default time=345
        hc.add(PingReply.createExternal(PingRequest.UDP_GUID.bytes(), (byte)7, 6342,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, 1000, false));
        
        // duplicate
        hc.add(PingReply.createExternal(PingRequest.UDP_GUID.bytes(), (byte)7, 6342,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, 1000, false));  
        hc.add(PingReply.createExternal(PingRequest.UDP_GUID.bytes(), (byte)7, 6343,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)143}, 30, false));
        // duplicate (well, with lower uptime)
        hc.add(PingReply.createExternal(PingRequest.UDP_GUID.bytes(), (byte)7, 6343,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)143}, 30, false));
        
        // private address (ignored)
        hc.add(PingReply.createExternal(PingRequest.UDP_GUID.bytes(), (byte)7, 6343,
            new byte[] {(byte)192, (byte)168, (byte)0, (byte)1}, 3000, false));
            
        // udp host caches ..
        hc.add(new ExtendedEndpoint("1.2.3.4", 6346).setUDPHostCache(true), false);
        hc.add(new ExtendedEndpoint("1.2.3.5", 6341).setUDPHostCache(true), false);
            
        File tmp=File.createTempFile("hc_test", ".net" );
        hc.write(tmp);

        //2. read HC from file.
        setUp(); // make sure we clear from memory the stuff we just added.
        UDPHostCache uhc = (UDPHostCache)PrivilegedAccessor.getValue(hc, "udpHostCache");
        assertEquals(0, uhc.getSize());
        assertEquals(0, hc.getNumHosts());
        hc.read(tmp);
        assertEquals(2, uhc.getSize());        
        assertEquals(3, hc.getNumHosts());
        assertEquals(new Endpoint("18.239.0.142", 6342),
                     hc.getAnEndpoint());
        assertEquals(new Endpoint("18.239.0.141", 6341),
                     hc.getAnEndpoint());
        assertEquals(new Endpoint("18.239.0.143", 6343),
                     hc.getAnEndpoint());
        assertEquals(0, hc.getNumHosts());
        //Cleanup.
        tmp.delete();
    }

    /** Tests that only the best hosts are remembered.  */
    public void testBestPermanent() throws Exception  {  
        HostCatcher.DEBUG=false;  //Too darn slow
        //1. Fill up host catcher with PERMANENT_SIZE+1 mid-level pongs
        //(various uptimes).
        final int N=HostCatcher.PERMANENT_SIZE;
        for (int i=0; i<=N; i++) {            
            hc.add(PingReply.createExternal(PingRequest.UDP_GUID.bytes(), (byte)7, i+1,
                new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                    i+10, false));
        }
        //Now add bad pong--which isn't really added
        hc.add(PingReply.createExternal(PingRequest.UDP_GUID.bytes(), (byte)7, N+2,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, 0, false));
        //Now re-add port 1 (which was kicked out earlier).
        hc.add(PingReply.createExternal(PingRequest.UDP_GUID.bytes(),(byte)7, 1,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, N+101,false));

        File tmp=File.createTempFile("hc_test", ".net" );
        hc.write(tmp);            

        //2. Read
        setUp();
        HostCatcher.DEBUG=false;  //Too darn slow
        hc.read(tmp);
        assertEquals(0, hc.getNumUltrapeerHosts());
        assertEquals(new Endpoint("18.239.0.142", 1),
                     hc.getAnEndpoint());

        // Note that we only go to 1 (not 0) because we already extracted
        // a host in the line before this.
        for (int i=N; i > 1; i--) {
            assertGreaterThan("No more hosts after "+i, 0, hc.getNumHosts());
            assertEquals(new Endpoint("18.239.0.142", i+1),
                         hc.getAnEndpoint());
        }
        assertEquals("some hosts leftover", 0, hc.getNumHosts());

        //Cleanup.
        tmp.delete();
    }

    /** Test that connection history is recorded. */
    public void testDoneWithConnect() throws Exception {
        hc.add(new Endpoint("18.239.0.1"), true);  
        hc.add(new Endpoint("18.239.0.2"), true);  //will succeed
        hc.add(new Endpoint("18.239.0.3"), true);  //will fail

        ExtendedEndpoint e3=(ExtendedEndpoint)hc.getAnEndpoint();
        assertEquals(new Endpoint("18.239.0.3"), e3);
        ExtendedEndpoint e2=(ExtendedEndpoint)hc.getAnEndpoint();
        assertEquals(new Endpoint("18.239.0.2"), e2);

        //record success (increases priority)
        hc.doneWithConnect(e2, true); 
        //record failure (lowers priority) with alternate form of method
        hc.doneWithConnect(e3, false);
        //Garbage (ignored)
        hc.doneWithConnect(new Endpoint("1.2.3.4", 6346), false);  
        hc.doneWithConnect(new Endpoint("18.239.0.3", 6349), true); //port

        //Check that permanent hosts are re-arranged.
        //Note that iterator yields worst to best.
        Iterator iter=hc.getPermanentHosts();
        ExtendedEndpoint e=(ExtendedEndpoint)iter.next();
        assertEquals(new Endpoint("18.239.0.3"), e);
        assertTrue(!e.getConnectionSuccesses().hasNext());
        assertTrue(e.getConnectionFailures().hasNext());

        e=(ExtendedEndpoint)iter.next();
        assertEquals(new Endpoint("18.239.0.1"), e);
        assertTrue(!e.getConnectionSuccesses().hasNext());
        assertTrue(!e.getConnectionFailures().hasNext());

        e=(ExtendedEndpoint)iter.next();
        assertEquals(new Endpoint("18.239.0.2"), e);
        assertTrue(e.getConnectionSuccesses().hasNext());
        assertTrue(!e.getConnectionFailures().hasNext());
    }

    public void testBadGnutellaDotNet() throws Exception {
        //System.out.println("-Testing bad Gnutella.net");
        //1. Write (mostly) corrupt file
        File tmp=File.createTempFile("hc_test", ".net" );
        FileWriter out=new FileWriter(tmp);
        out.write("18.239.0.141\n");                  //GOOD: port optional
        out.write("\n");                              //blank line
        out.write("18.239.0.144:A\n");                //bad port
        out.write("18.239.0.141:6347 A\n");           //bad uptime
        out.write("<html>total crap\n");              //not even close!
        out.write("  some garbage,1000,a,b,c,d,e,f,g\n");   //bad address
        out.write("18.239.0.142:6342,1000,a,b,c,d,e,f,g\n");//GOOD: ignore extra
        out.flush();
        out.close();

        //2. Read and verify
        setUp();
        hc.read(tmp);
        assertEquals("unexpected host",
            new Endpoint("18.239.0.142", 6342), hc.getAnEndpoint());
        assertEquals("unexpected host",
            new Endpoint("18.239.0.141", 6346), hc.getAnEndpoint());
        assertEquals("unexpected number of hosts", 0, hc.getNumHosts());
        assertEquals("unexpected number of ultrapeers",
            0, hc.getNumUltrapeerHosts());

        //Clean up
        tmp.delete();
    }
    
    public void testUDPHostCacheAdded() throws Exception {
        UDPHostCache uhc = (UDPHostCache)PrivilegedAccessor.getValue(hc, "udpHostCache");
        assertEquals(0, hc.getNumHosts());
        assertEquals(0, uhc.getSize());
        
        // Test with UDPHC pongs.
        GGEP ggep = new GGEP(true);
        ggep.put(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
        PingReply pr = PingReply.create(PingRequest.UDP_GUID.bytes(), (byte)1, 1,
                    new byte[] { 1, 1, 1, 1 },
                    (long)0, (long)0, false, ggep);
        hc.add(pr);
        
        assertEquals(0, hc.getNumHosts());
        assertEquals(1, uhc.getSize());
        
        // Test with an endpoint.
        ExtendedEndpoint ep = new ExtendedEndpoint("3.2.3.4", 6346);
        ep.setUDPHostCache(true);
        hc.add(ep, false);
        assertEquals(0, hc.getNumHosts());
        assertEquals(2, uhc.getSize());
        
        // Test with a name in the cache.
        ggep = new GGEP(true);
        ggep.put(GGEP.GGEP_HEADER_UDP_HOST_CACHE, "www.limewire.org");
        pr = PingReply.create(PingRequest.UDP_GUID.bytes(), (byte)1, 1,
                    new byte[] { 5, 4, 3, 2 },
                    (long)0, (long)0, false, ggep);
        hc.add(pr);
        assertEquals(0, hc.getNumHosts());
        assertEquals(3, uhc.getSize());
        
        Set s = (Set)PrivilegedAccessor.getValue(uhc, "udpHostsSet");
        // assert that it had all our endpoints.
        assertContains(s, new ExtendedEndpoint("1.1.1.1", 1));
        assertContains(s, new ExtendedEndpoint("3.2.3.4", 6346));
        assertContains(s, new ExtendedEndpoint("www.limewire.org", 1));
    }
    
    public void testPackedIPPongsAreUsed() throws Exception {
        assertEquals(0, hc.getNumHosts());
        
        GGEP ggep = new GGEP(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        PingReply pr = PingReply.create(
                PingRequest.UDP_GUID.bytes(), (byte)1, 1, new byte[] { 4, 3, 2, 1 },
            0, 0, false, ggep);
        
        hc.add(pr);
        assertEquals(5, hc.getNumHosts());
    }
    
    public void testPackedIPsWithUHC() throws Exception {
        UDPHostCache uhc = (UDPHostCache)PrivilegedAccessor.getValue(hc, "udpHostCache");
        assertEquals(0, hc.getNumHosts());
        assertEquals(0, uhc.getSize());
        
        GGEP ggep = new GGEP(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        ggep.put(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
        PingReply pr = PingReply.create(
                PingRequest.UDP_GUID.bytes(), (byte)1, 1, new byte[] { 4, 3, 2, 1 },
            0, 0, false, ggep);
        
        hc.add(pr);
        assertEquals(4, hc.getNumHosts());
        assertEquals(1, uhc.getSize());
    }
    
    public void testPackedHostCachesAreStored() throws Exception {
        UDPHostCache uhc = (UDPHostCache)PrivilegedAccessor.getValue(hc, "udpHostCache");
        assertEquals(0, hc.getNumHosts());
        assertEquals(0, uhc.getSize());
        
        GGEP ggep = new GGEP(true);
        String addrs ="1.2.3.4:81\n" +
        	"www.limewire.com:6379\n"+
        	"www.eff.org\n"+
            "www.test.org:1";
        ggep.putCompressed(GGEP.GGEP_HEADER_PACKED_HOSTCACHES, addrs.getBytes());
        PingReply pr = PingReply.create(
                PingRequest.UDP_GUID.bytes(), (byte)1, 1, new byte[] { 4, 3, 2, 1 },
            0, 0, false, ggep);
        
        hc.add(pr);
        assertEquals(1, hc.getNumHosts());
        assertEquals(4, uhc.getSize());
    }
    
    public void testAsyncEndpointObserver() throws Exception {
        StubEndpointObserver observer = new StubEndpointObserver();
        assertEquals(0, hc.getNumHosts());
        assertNull(observer.getEndpoint());
        hc.getAnEndpoint(observer);
        assertNull(observer.getEndpoint());
        Endpoint p = new Endpoint("231.123.254.1", 1);
        hc.add(p, true);
        assertEquals(p, observer.getEndpoint());
    }
    
    public void testMultipleAsyncEndpointObservers() throws Exception {
        StubEndpointObserver o1 = new StubEndpointObserver();
        StubEndpointObserver o2 = new StubEndpointObserver();
        
        assertEquals(0, hc.getNumHosts());
        hc.getAnEndpoint(o1);
        hc.getAnEndpoint(o2);
        assertNull(o1.getEndpoint());
        assertNull(o2.getEndpoint());
        Endpoint p1 = new Endpoint("231.123.254.1", 1);
        Endpoint p2 = new Endpoint("231.123.254.1", 2);
        hc.add(p1, true);
        hc.add(p2, true);
        assertEquals(p1, o1.getEndpoint());
        assertEquals(p2, o2.getEndpoint());
    }
    
    public void testAsyncEndpointObserverEndpointAlreadyAdded() throws Exception {
        StubEndpointObserver observer = new StubEndpointObserver();
        assertEquals(0, hc.getNumHosts());
        assertNull(observer.getEndpoint());
        Endpoint p = new Endpoint("231.123.254.1", 1);
        hc.add(p, true);
        hc.getAnEndpoint(observer);
        assertEquals(p, observer.getEndpoint());       
    }
        
   
    private static class StubGWebBootstrapper extends BootstrapServerManager {
        private boolean fetched = false;
        private String host = "123.234.132.143";
        
        public int fetchEndpointsAsync() {
            fetched = true;
            Endpoint ep = new Endpoint(host, 6346);
            RouterService.getHostCatcher().add(ep, false);
            
            return FETCH_IN_PROGRESS;
        }
    }
    
    private static class StubUDPBootstrapper extends UDPHostCache {
        private boolean fetched = false;
        private String host = "143.123.234.132";
        private boolean expired = false;
        
        public StubUDPBootstrapper() {
            super(new UDPPinger());
        }
        
        public boolean fetchHosts() {
            if(expired)
                return false;
            expired = true;
            fetched = true;
            Endpoint ep = new Endpoint(host, 6346);
            RouterService.getHostCatcher().add(ep, false);
            return true;
        }
        
        public int getSize() {
            if(expired)
                return 0;
            else
                return 1;
        }
    }
}
