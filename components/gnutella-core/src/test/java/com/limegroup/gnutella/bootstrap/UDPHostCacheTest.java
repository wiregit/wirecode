package com.limegroup.gnutella.bootstrap;

import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

import junit.framework.Test;

import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.UDPHostRanker;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.StandardMessageRouter;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 * Unit tests for UDPHostCache.
 */
public class UDPHostCacheTest extends BaseTestCase {
    private StubCache cache;
    
    public UDPHostCacheTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UDPHostCacheTest.class);
    }
    
    public static void globalSetUp() throws Exception {
        RouterService rs = new RouterService(new ActivityCallbackStub(),
                                             new StandardMessageRouter());
        RouterService.getAcceptor().setAddress(InetAddress.getByName("1.1.1.1"));
        
        UDPService.instance().start();
        
        DatagramSocket ds = (DatagramSocket)PrivilegedAccessor.invokeMethod(
                UDPService.instance(),
                "newListeningSocket",
                new Object[] { new Integer(7000) },
                new Class[] { Integer.TYPE } );
                                
        PrivilegedAccessor.invokeMethod(
                UDPService.instance(),
                "setListeningSocket",
                ds);
                
        RouterService.getMessageRouter().initialize();
    }
        
    
    public void setUp() {
        // use a really tiny expiry time
        cache = new StubCache();
    }

    public void testMaximumStored() {
        assertEquals(0, cache.getSize());
        
        for(int i = 0; i < 200; i++) {
            cache.add(create("1.2.3." + i));
            assertEquals(Math.min(i+1, 100), cache.getSize());
        }
    }
    
    public void testAddAndRemove() {
        assertEquals(0, cache.getSize());
        
        for(int i = 0; i < 50; i++) {
            cache.add(create("1.2.3." + i));
            assertEquals(i+1, cache.getSize());
        }
        
        for(int i = 0; i < 50; i++) {
            cache.remove(create("1.2.3." + i));
            assertEquals(50 - (i+1), cache.getSize());
        }
        
        // make sure we can remove stuff that doesn't exist
        // with no harm done.
        for(int i = 0; i < 50; i++) {
            assertEquals(0, cache.getSize());
            cache.remove(create("5.4.3.2" + i));
        }
    }
    
    public void testUsesTenAtATime() {
        assertEquals(0, cache.getSize());
        
        for(int i = 0; i < 20; i++)
            cache.add(create("1.2.3." + i));
        assertEquals(20, cache.getSize());
        
        assertEquals(-1, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(10, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(10, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
        
        // add newer hosts, should use them.
        for(int i = 0; i < 5; i++)
            cache.add(create("2.3.4." + i));
        assertEquals(25, cache.getSize());
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
        
        // add hosts we already added, shouldn't do nothin' with them
        for(int i = 0; i < 20; i++)
            cache.add(create("1.2.3." + i));
        for(int i = 0; i < 5; i++)
            cache.add(create("2.3.4." + i));
        assertEquals(25, cache.getSize());
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
    }
    
    public void testAttemptedExpiresAfterTime() throws Exception {
        assertEquals(0, cache.getSize());

        for(int i = 0; i < 20; i++)
            cache.add(create("1.2.3." + i));
        assertEquals(20, cache.getSize());
        
        assertEquals(-1, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(10, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(10, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
        
        // Wait the attempted expiry time.
        Thread.sleep(StubCache.EXPIRY_TIME + 1000); // +1000 for fudging time.
        cache.fetchHosts();
        assertEquals(10, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(10, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
    }
    
    public void testResetDataStartsFresh() {
        assertEquals(0, cache.getSize());

        for(int i = 0; i < 20; i++)
            cache.add(create("1.2.3." + i));
        assertEquals(20, cache.getSize());
        
        assertEquals(-1, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(10, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(10, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
        
        cache.resetData();
        
        cache.fetchHosts();
        assertEquals(10, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(10, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);        
    }
    
    public void testWriting() throws Exception {
        assertEquals(0, cache.getSize());
        StringWriter writer = new StringWriter();
        
        cache.write(writer);
        assertEquals("", writer.toString());
        
        for(int i = 0; i < 20; i++)
            cache.add(create("1.2.3." + i));
        assertEquals(20, cache.getSize());
        cache.write(writer);
        String written = writer.toString();
        Set readEPs = new HashSet();
        BufferedReader reader = new BufferedReader(new StringReader(written));
        for(int i = 0; i < 20; i++) {
            String read = reader.readLine();
            assertNotNull(read);
            assertNotEquals("", read);
            ExtendedEndpoint ep = ExtendedEndpoint.read(read);
            assertTrue(ep.isUDPHostCache());
            readEPs.add(ep);
        }
        for(int i = 0; i < 20; i++) {
            assertEquals(20-i, readEPs.size());
            ExtendedEndpoint ep = create("1.2.3." + i);
            assertContains(readEPs, ep);
            readEPs.remove(ep);
        }
    }
    
    public void testWritingPreservesDNSNames() throws Exception {
        assertEquals(0, cache.getSize());
        StringWriter writer = new StringWriter();
        
        cache.add(create("www.limewire.com"));
        cache.add(create("1.2.3.4"));
        cache.add(create("www.eff.org"));
        cache.write(writer);
        String written = writer.toString();
        BufferedReader reader = new BufferedReader(new StringReader(written));
        String read = reader.readLine();
        assertTrue(read, read.startsWith("www.limewire.com:6346"));
        read = reader.readLine();
        assertTrue(read, read.startsWith("1.2.3.4:6346"));
        read = reader.readLine();
        assertTrue(read, read.startsWith("www.eff.org:6346"));
        read = reader.readLine();
    }
    
    public void testRecordingFailuresAndSuccesses() throws Exception {
        assertEquals(0, cache.getSize());
        UDPHostRanker.LISTEN_EXPIRE_TIME = 3 * 1000;
        cache.doRealFetch = true;
        
        ExtendedEndpoint e1 = create("1.2.3.4");
        ExtendedEndpoint e2 = create("2.3.4.5");
        ExtendedEndpoint e3 = create("3.4.5.6");
        ExtendedEndpoint e4 = create("4.5.6.7");
        ExtendedEndpoint e5 = create("5.6.7.8");
        
        cache.add(e1);
        cache.add(e2);
        cache.add(e3);
        cache.add(e4);
        cache.add(e5);
        assertEquals(5, cache.getSize());
        
        // this will cause UDPHostRanker to send out Pings.
        cache.fetchHosts();
        Thread.sleep(100);
        
        // Have MessageRouter try and route some replies from some hosts...
        routeFor("1.2.3.4", cache.guid);
        routeFor("3.4.5.6", cache.guid);
        routeFor("5.6.7.8", cache.guid);
        
        // sleep until the MessageListener is unregistered,
        // recording the successes & failures.
        Thread.sleep(4 * 1000);
        
        assertEquals(0, e1.getUDPHostCacheFailures());
        assertEquals(1, e2.getUDPHostCacheFailures());
        assertEquals(0, e3.getUDPHostCacheFailures());
        assertEquals(1, e4.getUDPHostCacheFailures());
        assertEquals(0, e5.getUDPHostCacheFailures());
        assertEquals(5, cache.getSize());
        
        // reset data, try again.
        cache.resetData();
        cache.fetchHosts();
        Thread.sleep(100);
        routeFor("1.2.3.4", cache.guid);
        routeFor("2.3.4.5", cache.guid);
        routeFor("5.6.7.8", cache.guid);
        Thread.sleep(4 * 1000);
        assertEquals(0, e1.getUDPHostCacheFailures());
        assertEquals(0, e2.getUDPHostCacheFailures());
        assertEquals(1, e3.getUDPHostCacheFailures());
        assertEquals(2, e4.getUDPHostCacheFailures());
        assertEquals(0, e5.getUDPHostCacheFailures());
        assertEquals(5, cache.getSize());
        
        // reset data, try again.
        cache.resetData();
        cache.fetchHosts();
        Thread.sleep(100);        
        routeFor("3.4.5.6", cache.guid);
        Thread.sleep(4 * 1000);
        assertEquals(1, e1.getUDPHostCacheFailures());
        assertEquals(1, e2.getUDPHostCacheFailures());
        assertEquals(0, e3.getUDPHostCacheFailures());
        assertEquals(3, e4.getUDPHostCacheFailures());
        assertEquals(1, e5.getUDPHostCacheFailures());
        assertEquals(5, cache.getSize());
        
        // reset data, try again.
        cache.resetData();
        cache.fetchHosts();
        Thread.sleep(100);        
        routeFor("1.2.3.4", cache.guid);
        routeFor("2.3.4.5", cache.guid);
        Thread.sleep(4 * 1000);
        assertEquals(0, e1.getUDPHostCacheFailures());
        assertEquals(0, e2.getUDPHostCacheFailures());
        assertEquals(1, e3.getUDPHostCacheFailures());
        assertEquals(4, e4.getUDPHostCacheFailures());
        assertEquals(2, e5.getUDPHostCacheFailures());
        assertEquals(5, cache.getSize());
        
        // reset data, try again.
        cache.resetData();
        cache.fetchHosts();
        Thread.sleep(100);        
        routeFor("1.2.3.4", cache.guid);
        routeFor("2.3.4.5", cache.guid);
        Thread.sleep(4 * 1000);
        assertEquals(0, e1.getUDPHostCacheFailures());
        assertEquals(0, e2.getUDPHostCacheFailures());
        assertEquals(2, e3.getUDPHostCacheFailures());
        assertEquals(5, e4.getUDPHostCacheFailures());
        assertEquals(3, e5.getUDPHostCacheFailures());
        assertEquals(5, cache.getSize());
        
        // reset data, try again.
        // e4 should be ejected because it failed over 5 times.
        cache.resetData();
        cache.fetchHosts();
        Thread.sleep(100);        
        routeFor("1.2.3.4", cache.guid);
        routeFor("2.3.4.5", cache.guid);
        Thread.sleep(4 * 1000);
        assertEquals(0, e1.getUDPHostCacheFailures());
        assertEquals(0, e2.getUDPHostCacheFailures());
        assertEquals(3, e3.getUDPHostCacheFailures());
        assertEquals(6, e4.getUDPHostCacheFailures());
        assertEquals(4, e5.getUDPHostCacheFailures());
        assertEquals(4, cache.getSize());        
    }
    
    private void routeFor(String host, byte[] guid) throws Exception {
        PingReply pr = PingReply.create(guid, (byte)1);
        DatagramPacket dp = new DatagramPacket(new byte[0], 0);
        dp.setAddress(InetAddress.getByName(host));
        dp.setPort(6346);
        
        RouterService.getMessageRouter().handleUDPMessage(pr, dp);
    }    
    
    private ExtendedEndpoint create(String host) {
        return (new ExtendedEndpoint(host, 6346)).setUDPHostCache(true);
    }
    
    private static class StubCache extends UDPHostCache {
        private static final int EXPIRY_TIME = 10 * 1000;        
        private int amountFetched = -1;
        private Collection lastFetched;
        private boolean doRealFetch = false;
        private byte[] guid = null;
        
        public StubCache() {
            super(EXPIRY_TIME);
        }
        
        protected boolean fetch(Collection hosts) {
            if(doRealFetch) {
                return super.fetch(hosts);
            } else {
                amountFetched = hosts.size();
                lastFetched = hosts;
                if(amountFetched == 0)
                    return false;
                else
                    return true;
            }
        }
        
        protected PingRequest getPing() {
            PingRequest pr = super.getPing();
            guid = pr.getGUID();
            return pr;
        }
    }
}