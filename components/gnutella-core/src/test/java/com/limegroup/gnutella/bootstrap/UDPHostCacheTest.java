package com.limegroup.gnutella.bootstrap;

import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import junit.framework.Test;

import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.StandardMessageRouter;
import com.limegroup.gnutella.UniqueHostPinger;
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
        
        DatagramSocket ds = (DatagramSocket)PrivilegedAccessor.invokeMethod(
                UDPService.instance(),
                "newListeningSocket",
                new Object[] { new Integer(7000) },
                new Class[] { Integer.TYPE } );
                                
        PrivilegedAccessor.invokeMethod(
                UDPService.instance(),
                "setListeningSocket",
                new Object[] { ds } ,
                new Class[] { DatagramSocket.class });
                
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
    
    public void testUsesFiveAtATime() {
        assertEquals(0, cache.getSize());
        
        for(int i = 0; i < 23; i++)
            cache.add(create("1.2.3." + i));
        assertEquals(23, cache.getSize());
        
        assertEquals(-1, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(3, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
        
        // add newer hosts, should use them.
        for(int i = 0; i < 5; i++)
            cache.add(create("2.3.4." + i));
        assertEquals(28, cache.getSize());
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
        
        // add hosts we already added, shouldn't do nothin' with them
        for(int i = 0; i < 23; i++)
            cache.add(create("1.2.3." + i));
        for(int i = 0; i < 5; i++)
            cache.add(create("2.3.4." + i));
        assertEquals(28, cache.getSize());
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
    }
    
    public void testUsesFiveBestAtATime() {
        assertEquals(0, cache.getSize());
        
        List endpoints = new LinkedList();
        
        // add 8 hosts with 0 failures, 3 with 1 failure,
        // 5 with 2 failures, 4 with 3 failures,
        // and 7 with 4 failures -- we should get'm in the right order
        // regardless of how they were added.
        int i = 0;
        for(; i < 8; i++)
            endpoints.add(create("1.2.3." + i, 0));
        for(; i < 8+3; i++)
            endpoints.add(create("1.2.3." + i, 1));
        for(; i < 8+3+5; i++)
            endpoints.add(create("1.2.3." + i, 2));
        for(; i < 8+3+5+4; i++)
            endpoints.add(create("1.2.3." + i, 3));
        for(; i < 8+3+5+4+7; i++)
            endpoints.add(create("1.2.3." + i, 4));
        
        // make sure they're in random order, then add them to the cache.    
        Collections.shuffle(endpoints);
        for(Iterator iter = endpoints.iterator(); iter.hasNext(); )
            cache.add((ExtendedEndpoint)iter.next());
        
        assertEquals(-1, cache.amountFetched);    
        assertEquals(8+3+5+4+7, cache.getSize());
        
        // Fetch until we run out, adding them all to a list
        // in order we fetched'm.  (Note that we already have
        // a test that makes sure we only do 5 at a time --
        // this test just ensures we do them in the right order.)
        List fetchedHosts = new ArrayList();
        while(true) {
            cache.fetchHosts();
            if(cache.amountFetched == 0)
                break;
            fetchedHosts.addAll(cache.lastFetched);
        }
        
        int max = 8+3+5+4+7;
        assertEquals(max, fetchedHosts.size());
        for(i = 0; i < 8+3+5+4+7; i++) {
            ExtendedEndpoint ep = (ExtendedEndpoint)fetchedHosts.get(i);
            if(i < 8)
                assertEquals(0, ep.getUDPHostCacheFailures());
            else if(i < 8+3)
                assertEquals(1, ep.getUDPHostCacheFailures());
            else if(i < 8+3+5)
                assertEquals(2, ep.getUDPHostCacheFailures());
            else if(i < 8+3+5+4)
                assertEquals(3, ep.getUDPHostCacheFailures());
            else if(i < 8+3+5+4+7)
                assertEquals(4, ep.getUDPHostCacheFailures());
            else
                fail("wrong i: " + i);
        }
    }
            
    
    public void testAttemptedExpiresAfterTime() throws Exception {
        assertEquals(0, cache.getSize());

        for(int i = 0; i < 13; i++)
            cache.add(create("1.2.3." + i));
        assertEquals(13, cache.getSize());
        
        assertEquals(-1, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(3, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
        
        // Wait the attempted expiry time.
        Thread.sleep(StubCache.EXPIRY_TIME + 1000); // +1000 for fudging time.
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(3, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
    }
    
    public void testResetDataStartsFresh() {
        assertEquals(0, cache.getSize());

        for(int i = 0; i < 8; i++)
            cache.add(create("1.2.3." + i));
        assertEquals(8, cache.getSize());
        
        assertEquals(-1, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(3, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(0, cache.amountFetched);
        
        cache.resetData();
        
        cache.fetchHosts();
        assertEquals(5, cache.amountFetched);
        cache.fetchHosts();
        assertEquals(3, cache.amountFetched);
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
        List readLines = new LinkedList();
        readLines.add(reader.readLine());
        readLines.add(reader.readLine());
        readLines.add(reader.readLine());
        assertStartsWithInList("www.eff.org:6346", readLines);
        assertStartsWithInList("1.2.3.4:6346", readLines);
        assertStartsWithInList("www.limewire.com:6346", readLines);
        assertEquals(readLines.toString(), 0, readLines.size());
        assertEquals(null, reader.readLine());
    }
    
    public void testRecordingFailuresAndSuccesses() throws Exception {
        assertEquals(0, cache.getSize());
        UDPPinger.LISTEN_EXPIRE_TIME = 3 * 1000;
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
        
        // now try a real reset data that decrements failures,
        // ensure the caches failures went down 
        // this includes e4, because we did attempt it.
        cache.doRealDecrement = true;
        cache.resetData();
        assertEquals(0, e1.getUDPHostCacheFailures());
        assertEquals(0, e2.getUDPHostCacheFailures());
        assertEquals(2, e3.getUDPHostCacheFailures());
        assertEquals(5, e4.getUDPHostCacheFailures());
        assertEquals(3, e5.getUDPHostCacheFailures());
        assertEquals(5, cache.getSize()); // e4 was readded.
    }
    
    private void assertStartsWithInList(String find, List list) throws Exception {
        boolean found = false;
        for(Iterator i = list.iterator(); i.hasNext();) {
            String read = (String)i.next();
            if(read.startsWith(find)) {
                found = true;
                i.remove();
                break;
            }
        }
        assertTrue("missing: " + find + ", had: " + list, found);
    }    
    
    private void routeFor(String host, byte[] guid) throws Exception {
        PingReply pr = PingReply.create(guid, (byte)1);
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName(host), 6346);
        
        RouterService.getMessageRouter().handleUDPMessage(pr, addr);
    }    
    
    private ExtendedEndpoint create(String host) {
        return (new ExtendedEndpoint(host, 6346)).setUDPHostCache(true);
    }
    
    private ExtendedEndpoint create(String host, int failures) {
        ExtendedEndpoint ep = create(host);
        for(int i = 0; i < failures; i++)
            ep.recordUDPHostCacheFailure();
        return ep;
    }
    
    private static class StubCache extends UDPHostCache {
        private static final int EXPIRY_TIME = 10 * 1000;        
        private int amountFetched = -1;
        private Collection lastFetched;
        private boolean doRealFetch = false;
        private byte[] guid = null;
        private boolean doRealDecrement = false;
        
        public StubCache() {
            super(EXPIRY_TIME,new UniqueHostPinger());
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
        
        protected void decrementFailures() {
            if(doRealDecrement) {
                super.decrementFailures();
            } else {
                ;
            }
        }
    }
}