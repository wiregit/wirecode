package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.collection.FixedsizePriorityQueue;
import org.limewire.io.IpPortImpl;
import org.limewire.util.ByteOrder;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.bootstrap.UDPHostCache;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;


@SuppressWarnings( { "unchecked", "cast" } )
public class HostCatcherTest extends LimeTestCase {  
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
    public static void globalSetUp() throws Exception {
        new RouterService( new ActivityCallbackStub() );
    }
    
    /**
     * Returns a new HostCatcher connected to stubs. YOU MAY WANT TO CALL EXPIRE to force bootstrap pongs.
     */
    public void setUp() {
        // explicitly allow all ips to test.
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[] {});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] { "*.*" });

        HostCatcher.DEBUG = true;

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
     * Tests to make sure that the UDP Host Cache is used  
     * if we know of any host caches.
     */
    public void testUDPCachesUsed() throws Exception {
        assertEquals(0, hc.getNumHosts());
        PrivilegedAccessor.setValue(RouterService.class, "catcher", hc);        
        
        StubUDPBootstrapper udp = new StubUDPBootstrapper();
        PrivilegedAccessor.setValue(hc, "udpHostCache", udp);
        
        Endpoint firstHost = hc.getAnEndpoint();
        assertTrue(udp.fetched);
        assertEquals(udp.host, firstHost.getAddress());
        udp.fetched = false;
        
        // Since udp was done quickly and only gave us one host (and we
        // just used it), the next request will spark a GW request.
        Endpoint second = hc.getAnEndpointImmediate(null);
        assertNull(second);
        Thread.sleep(5000); // just to make sure it doesn't trigger a fetch later
        assertFalse(udp.fetched);
        
        udp.expired = false;
        
        // Now another fetch will wait until time passes enough to retry
        // udp (too long before retrying a GW)
        Endpoint thirdHost = hc.getAnEndpoint();
        assertTrue(udp.fetched);
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
        hc.add(ProviderHacks.getPingReplyFactory().createExternal(new byte[16], (byte)3, 6346, 
            new byte[] {(byte)192,(byte)168,(byte)0,(byte)1}, false));
        assertEquals("private PingReply added as ultrapeer",
					 0 ,hc.getNumUltrapeerHosts());

        setUp();
        hc.add(ProviderHacks.getPingReplyFactory().createExternal(new byte[16], (byte)3, 6346, 
            new byte[] {(byte)18,(byte)239,(byte)0,(byte)1}, false));
        assertEquals("normal PingReply added as ultrapeer",
                0, hc.getNumUltrapeerHosts());


        setUp();
        hc.add(ProviderHacks.getPingReplyFactory().createExternal(new byte[16], (byte)3, 6346, 
            new byte[] {(byte)18,(byte)239,(byte)0,(byte)1}, true));
        assertEquals("ultrapeer PingReply not added as ultrapeer",
                1, hc.getNumUltrapeerHosts());
    }


    public void testPermanent() throws Exception {
        //Systm.out.println("-Testing write of permanent nodes to Gnutella.net");
        //1. Create HC, add entries, write to disk.
        hc.add(new Endpoint("18.239.0.141", 6341), false);//default time=345
        hc.add(ProviderHacks.getPingReplyFactory().createExternal(GUID.makeGuid(), (byte)7, 6342,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, 1000, false));
        
        // duplicate
        hc.add(ProviderHacks.getPingReplyFactory().createExternal(GUID.makeGuid(), (byte)7, 6342,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, 1000, false));  
        hc.add(ProviderHacks.getPingReplyFactory().createExternal(GUID.makeGuid(), (byte)7, 6343,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)143}, 30, false));
        // duplicate (well, with lower uptime)
        hc.add(ProviderHacks.getPingReplyFactory().createExternal(GUID.makeGuid(), (byte)7, 6343,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)143}, 30, false));
        
        // private address (ignored)
        hc.add(ProviderHacks.getPingReplyFactory().createExternal(GUID.makeGuid(), (byte)7, 6343,
            new byte[] {(byte)192, (byte)168, (byte)0, (byte)1}, 3000, false));
            
        // udp host caches ..
        hc.add(new ExtendedEndpoint("1.2.3.4", 6346).setUDPHostCache(true), false);
        hc.add(new ExtendedEndpoint("1.2.3.5", 6341).setUDPHostCache(true), false);
        
        // dht capable node
        ExtendedEndpoint ep = new ExtendedEndpoint("18.239.0.100", 6323, 3);
        ep.setDHTVersion(2);
        ep.setDHTMode(DHTMode.INACTIVE);
        hc.add(ep, false);
        // dht active node
        ep = new ExtendedEndpoint("18.239.0.101", 6322, 2);
        ep.setDHTVersion(1);
        ep.setDHTMode(DHTMode.ACTIVE);
        hc.add(ep, false);
            
        File tmp=File.createTempFile("hc_test", ".net" );
        hc.write(tmp);

        //2. read HC from file.
        setUp(); // make sure we clear from memory the stuff we just added.
        UDPHostCache uhc = (UDPHostCache)PrivilegedAccessor.getValue(hc, "udpHostCache");
        assertEquals(0, uhc.getSize());
        assertEquals(0, hc.getNumHosts());
        hc.read(tmp);
        assertEquals(2, uhc.getSize());        
        assertEquals(5, hc.getNumHosts());
        // the highest uptime hosts should be given out first,
        // but not in order
        Set<Endpoint> s = new HashSet<Endpoint>();
        s.add(hc.getAnEndpoint());
        s.add(hc.getAnEndpoint());
        s.add(hc.getAnEndpoint());
        assertTrue(s.contains(new Endpoint("18.239.0.142", 6342)));
        assertTrue(s.contains(new Endpoint("18.239.0.141", 6341)));
        assertTrue(s.contains(new Endpoint("18.239.0.143", 6343)));
        
        ExtendedEndpoint xep = (ExtendedEndpoint) hc.getAnEndpoint();
        assertTrue(xep.supportsDHT());
        assertEquals(xep.getDHTVersion(), 2);
        xep = (ExtendedEndpoint) hc.getAnEndpoint();
        assertTrue(xep.supportsDHT());
        assertEquals(xep.getDHTVersion(), 1);
        assertEquals(DHTMode.ACTIVE, xep.getDHTMode());
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
            hc.add(ProviderHacks.getPingReplyFactory().createExternal(GUID.makeGuid(), (byte)7, i+1,
                new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                    i+10, false));
        }
        //Now add bad pong--which isn't really added
        hc.add(ProviderHacks.getPingReplyFactory().createExternal(GUID.makeGuid(), (byte)7, N+2,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, 0, false));
        //Now re-add port 1 (which was kicked out earlier).
        hc.add(ProviderHacks.getPingReplyFactory().createExternal(GUID.makeGuid(), (byte)7, 1,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, N+101,false));

        File tmp=File.createTempFile("hc_test", ".net" );
        hc.write(tmp);            

        //2. Read
        setUp();
        HostCatcher.DEBUG=false;  //Too darn slow
        hc.read(tmp);
        assertEquals(0, hc.getNumUltrapeerHosts());

        Set<Endpoint> s = new HashSet<Endpoint>();
        // Note that we only go to 1 (not 0) because we already extracted
        // a host in the line before this.
        for (int i=N; i > 0; i--) 
            s.add(hc.getAnEndpoint());
        
        // the bad host should not be in there
        assertFalse(s.contains(new Endpoint("18.239.0.142",N+2)));
        // the good one should
        assertTrue(s.contains(new Endpoint("18.239.0.142",1)));
        
        assertEquals("some hosts leftover", 0, hc.getNumHosts());

        //Cleanup.
        tmp.delete();
    }
    
    /**
     * tests that when reading from disk, the best third of the hosts
     * will be given out first
     */
    public void testBestRestored() throws Exception {
        // add a bunch of bad hosts
        for (int i = 0; i < 10; i++)
            hc.add(new ExtendedEndpoint("1.1.1.1", 100+i, i), false);
        // a bunch of average hosts
        for (int i = 0; i < 10; i++)
            hc.add(new ExtendedEndpoint("2.2.2.2", 200+i, 200+i), false);
        // a bunch of good hosts
        for (int i = 0; i < 20; i++)
            hc.add(new ExtendedEndpoint("3.3.3.3", 300+i, 300+i), false);

        // reload
        File tmp=File.createTempFile("hc_test", ".net" );
        hc.write(tmp);            
        setUp();
        hc.read(tmp);
        tmp.delete();
        
        // the first 10 hosts should all be good
        for (int i = 0; i < 10; i++) 
            assertEquals("3.3.3.3",hc.getAnEndpoint().getAddress());
        
        // the next 10 should not have any bad ones
        Set<Endpoint> s = new HashSet<Endpoint>();
        for (int i = 0; i < 10; i++) 
            s.add(hc.getAnEndpoint());
        for (int i = 0; i < 10; i++) 
            assertFalse(s.contains(new Endpoint("1.1.1.1", 100+i)));
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
        Set<Endpoint> s = new HashSet<Endpoint>();
        s.add(hc.getAnEndpoint());
        s.add(hc.getAnEndpoint());
        assertTrue(s.contains(new Endpoint("18.239.0.142", 6342)));
        assertTrue(s.contains(new Endpoint("18.239.0.141", 6346)));
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
        GGEP ggep = new GGEP();
        ggep.put(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
        PingReply pr = ProviderHacks.getPingReplyFactory().create(GUID.makeGuid(), (byte)1, 1,
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
        ggep = new GGEP();
        ggep.put(GGEP.GGEP_HEADER_UDP_HOST_CACHE, "www.limewire.org");
        pr = ProviderHacks.getPingReplyFactory().create(GUID.makeGuid(), (byte)1, 1,
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
        ConnectionSettings.FILTER_CLASS_C.setValue(true);
        assertEquals(0, hc.getNumHosts());
        
        GGEP ggep = new GGEP();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 1, 1, 2, 1, 0 } ); // same class C - filtered
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        PingReply pr = ProviderHacks.getPingReplyFactory().create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 4, 3, 2, 1 },
            0, 0, false, ggep);
        
        hc.add(pr);
        assertEquals(5, hc.getNumHosts());
    }
    
    public void testPackedIPsWithUHC() throws Exception {
        UDPHostCache uhc = (UDPHostCache)PrivilegedAccessor.getValue(hc, "udpHostCache");
        assertEquals(0, hc.getNumHosts());
        assertEquals(0, uhc.getSize());
        
        GGEP ggep = new GGEP();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        ggep.put(GGEP.GGEP_HEADER_UDP_HOST_CACHE);
        PingReply pr = ProviderHacks.getPingReplyFactory().create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 4, 3, 2, 1 },
            0, 0, false, ggep);
        
        hc.add(pr);
        assertEquals(4, hc.getNumHosts());
        assertEquals(1, uhc.getSize());
    }
    
    public void testPackedHostCachesAreStored() throws Exception {
        UDPHostCache uhc = (UDPHostCache)PrivilegedAccessor.getValue(hc, "udpHostCache");
        assertEquals(0, hc.getNumHosts());
        assertEquals(0, uhc.getSize());
        
        GGEP ggep = new GGEP();
        String addrs ="1.2.3.4:81\n" +
        	"www.limewire.com:6379\n"+
        	"www.eff.org\n"+
            "www.test.org:1";
        ggep.putCompressed(GGEP.GGEP_HEADER_PACKED_HOSTCACHES, addrs.getBytes());
        PingReply pr = ProviderHacks.getPingReplyFactory().create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 4, 3, 2, 1 },
            0, 0, false, ggep);
        
        hc.add(pr);
        assertEquals(1, hc.getNumHosts());
        assertEquals(4, uhc.getSize());
    }
    
    public void testImmediateEndpointObserverNoHosts() throws Exception {
        StubEndpointObserver observer = new StubEndpointObserver();
        assertEquals(0, hc.getNumHosts());
        assertNull(observer.getEndpoint());
        assertNull(hc.getAnEndpointImmediate(observer));
        assertNull(observer.getEndpoint());
        Endpoint p = new Endpoint("231.123.254.1", 1);
        hc.add(p, true);
        assertEquals(p, observer.getEndpoint());
    }
    
    public void testImmediateEndpointObserverWithHosts() throws Exception {
        Endpoint p = new Endpoint("231.123.254.1", 1);
        hc.add(p, true);
        StubEndpointObserver observer = new StubEndpointObserver();
        assertEquals(1, hc.getNumHosts());
        assertNull(observer.getEndpoint());
        assertEquals(p, hc.getAnEndpointImmediate(observer));
        assertNull(observer.getEndpoint());
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
    
    public void testGetDHTSupportEndpoint() throws Exception {
        ConnectionSettings.FILTER_CLASS_C.setValue(true);
        assertEquals(0, hc.getDHTSupportEndpoint(0).size());
        
        // add a bunch of nodes from the same class C network
        ExtendedEndpoint ep;
        //dht nodes
        for(int i=6300; i < 6309 ; i++) {
            ep = new ExtendedEndpoint("18.239.0.100", i);
            ep.setDHTVersion(2);
            if((i % 2) == 0) {
                ep.setDHTMode(DHTMode.INACTIVE);
            } else {
                ep.setDHTMode(DHTMode.PASSIVE);
            }
            hc.add(ep, false);
        }
        
        List<ExtendedEndpoint> hostList = hc.getDHTSupportEndpoint(0);
        assertEquals(1, hostList.size());
        
        // add nodes from different class C networks
        for(int i=1; i < 9 ; i++) {
            ep = new ExtendedEndpoint("18.239."+i+".100", 6000);
            ep.setDHTVersion(2);
            if((i % 2) == 0) {
                ep.setDHTMode(DHTMode.INACTIVE);
            } else {
                ep.setDHTMode(DHTMode.PASSIVE);
            }
            hc.add(ep, false);
        }
        //dht active node
        ep = new ExtendedEndpoint("18.239.10.101", 6322);
        ep.setDHTVersion(1);
        ep.setDHTMode(DHTMode.ACTIVE);
        hc.add(ep, false);
        
        hostList = hc.getDHTSupportEndpoint(0);
        ep = hostList.get(0);
        assertEquals("18.239.10.101", ep.getAddress());
        assertTrue(ep.getDHTMode().equals(DHTMode.ACTIVE));
        
        hostList.remove(0);
        ep = hostList.iterator().next();
        assertTrue(ep.getDHTMode().equals(DHTMode.PASSIVE));
        ep = hostList.get(hostList.size()-1);
        assertFalse(ep.getDHTMode().equals(DHTMode.PASSIVE));
        assertTrue(ep.getDHTMode().equals(DHTMode.INACTIVE));
        
        //try excluding version
        hostList = hc.getDHTSupportEndpoint(2);
        assertEquals(9, hostList.size());
        assertTrue(hostList.get(0).getAddress().endsWith(".100"));
    }

    public void testPingTagging() throws Exception {
        ConnectionSettings.FILTER_CLASS_C.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        // tell hostcatcher about one node
        File tmp=new File(CommonUtils.getUserSettingsDir(), "gnutella.net" );
        FileWriter out=new FileWriter(tmp);
        out.write("127.0.0.1:6000,26694,1176133322250,1176133334390,,en,,,");    
        out.flush();
        out.close();
        DatagramSocket s = new DatagramSocket(6000);
        s.setSoTimeout(3000);
        PrivilegedAccessor.setValue(RouterService.class , "catcher", hc);
        PrivilegedAccessor.setValue(RouterService.class, "manager",new
                ConnectionManagerStub() {
            @Override
            public boolean isFullyConnected() {
                return false;
            }
            @Override
            public List<ManagedConnection> getInitializedConnections() {
                return Collections.emptyList();
            }
            @Override
            public int getPreferredConnectionCount() {
                return 1;
            }
        });
        
        // make it send udp pings
        ProviderHacks.getAcceptor().init();
        ProviderHacks.getAcceptor().start();
        RouterService.getMessageRouter().initialize();
        hc.expire();
        hc.sendUDPPings();
        
        // empty the hostcatcher
        StubEndpointObserver seo = new StubEndpointObserver();
        assertNotNull(hc.getAnEndpointImmediate(seo));
        assertNull(hc.getAnEndpointImmediate(seo));
        hc.removeEndpointObserver(seo);
        
        // receive the ping
        DatagramPacket p = new DatagramPacket(new byte[1000], 1000);
        s.receive(p);
        PingRequest ping = (PingRequest) MessageFactory.read(new ByteArrayInputStream(p.getData()));
        assertNotNull(ping);

        
        // this ping should be tagged
        byte [] expectedGUID = ProviderHacks.getUdpService().getSolicitedGUID().bytes();
        assertNotEquals(expectedGUID, ping.getGUID());
        
        // if a pong is sent from the same host, it will be processed
        byte [] payload = new byte[14];
        ByteOrder.short2leb((short)6000, payload, 0);
        System.arraycopy(InetAddress.getByName("127.0.0.1").getAddress(),0,payload,2,4);
        PingReply pong = 
            ProviderHacks.getPingReplyFactory().createFromNetwork(ping.getGUID().clone(), (byte)1, (byte)1, payload, Message.Network.UDP);
        ProviderHacks.getUdpService().processMessage(pong, 
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 6000));
        
        // the pong guid should now be restored to the solicited guid
        assertEquals(expectedGUID,pong.getGUID());
        
        Thread.sleep(1000);
        Endpoint e = hc.getAnEndpointImmediate(new StubEndpointObserver());
        assertNotNull(e);
        assertEquals("127.0.0.1",e.getAddress());
        assertEquals(6000, e.getPort());
        
        // however if a pong with the same guid arrives from
        // another address, it will be ignored
        System.arraycopy(InetAddress.getByName("127.0.0.2").getAddress(),0,payload,2,4);
        pong = 
            ProviderHacks.getPingReplyFactory().createFromNetwork(ping.getGUID().clone(), (byte)1, (byte)1, payload, Message.Network.UDP);
        ProviderHacks.getUdpService().processMessage(pong, 
                new InetSocketAddress(InetAddress.getByName("127.0.0.2"), 6000));
        
        // the guid of this pong will not be restored correctly
        assertNotEquals(expectedGUID,pong.getGUID());
        Thread.sleep(1000);
        assertEquals(0,hc.getNumHosts());

        tmp.delete();
    }
    
    public void testIsTLSCapable() throws Exception {
        assertEquals(0, hc.getNumHosts());
        ExtendedEndpoint p = new ExtendedEndpoint("231.123.254.1", 1);
        hc.add(p, true);
        assertFalse(hc.isHostTLSCapable(new IpPortImpl("231.123.254.1", 1)));
        assertFalse(hc.isHostTLSCapable(p));
        
        p = new ExtendedEndpoint("21.81.1.1", 1);
        p.setTLSCapable(true);
        hc.add(p, true);
        assertTrue(hc.isHostTLSCapable(new IpPortImpl("21.81.1.1", 1)));
        assertTrue(hc.isHostTLSCapable(p));
        
        p = new ExtendedEndpoint("1.2.3.101", 1);
        p.setTLSCapable(true);
        assertTrue(hc.isHostTLSCapable(p));
        
        // Hand-craft a PingReply w/ TLS IPPs to see if they're added as
        // TLS capable hosts.
        assertEquals(2, hc.getNumHosts());
        GGEP ggep = new GGEP();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        // mark the second & third items as TLS
        ggep.put(GGEP.GGEP_HEADER_PACKED_IPPORTS_TLS, (0x40 | 0x20));
        ggep.put(GGEP.GGEP_HEADER_TLS_CAPABLE); // mark this guy as TLS capable.
        PingReply pr = ProviderHacks.getPingReplyFactory().create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 0, 1, 0 },
            0, 0, false, ggep);
        hc.add(pr);
        assertEquals(7, hc.getNumHosts());
        assertFalse(hc.isHostTLSCapable(new IpPortImpl("1.1.1.1:1")));
        assertTrue(hc.isHostTLSCapable(new IpPortImpl("1.2.3.4:2")));
        assertTrue(hc.isHostTLSCapable(new IpPortImpl("3.4.2.3:3")));
        assertFalse(hc.isHostTLSCapable(new IpPortImpl("254.0.0.3:4")));
        assertTrue(hc.isHostTLSCapable(new IpPortImpl("1.0.1.0:1")));

        // The order of these checks are a little stricter than necessary
        Endpoint ep = hc.getAnEndpointImmediate(null);
        assertEquals("254.0.0.3", ep.getAddress());
        assertFalse(ep.isTLSCapable());
        
        ep = hc.getAnEndpointImmediate(null);
        assertEquals("3.4.2.3", ep.getAddress());
        assertTrue(ep.isTLSCapable());
        
        ep = hc.getAnEndpointImmediate(null);
        assertEquals("1.2.3.4", ep.getAddress());
        assertTrue(ep.isTLSCapable());
        
        ep = hc.getAnEndpointImmediate(null);
        assertEquals("1.1.1.1", ep.getAddress());
        assertFalse(ep.isTLSCapable());
        
        ep = hc.getAnEndpointImmediate(null);
        assertEquals("21.81.1.1", ep.getAddress());
        assertTrue(ep.isTLSCapable());
        
        ep = hc.getAnEndpointImmediate(null);
        assertEquals("231.123.254.1", ep.getAddress());
        assertFalse(ep.isTLSCapable());
        
        ep = hc.getAnEndpointImmediate(null);
        assertEquals("1.0.1.0", ep.getAddress());
        assertTrue(ep.isTLSCapable());
        
        assertNull(hc.getAnEndpointImmediate(null));
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
            ProviderHacks.getHostCatcher().add(ep, false);
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
