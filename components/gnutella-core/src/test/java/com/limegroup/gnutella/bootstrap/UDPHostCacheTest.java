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

import junit.framework.Test;

import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.util.BaseTestCase;

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
        
    
    private ExtendedEndpoint create(String host) {
        return (new ExtendedEndpoint(host, 6346)).setUDPHostCache(true);
    }
    
    private static class StubCache extends UDPHostCache {
        private static final int EXPIRY_TIME = 10 * 1000;        
        private int amountFetched = -1;
        private Collection lastFetched;
        
        public StubCache() {
            super(EXPIRY_TIME);
        }
        
        protected boolean fetch(Collection hosts) {
            amountFetched = hosts.size();
            lastFetched = hosts;
            if(amountFetched == 0)
                return false;
            else
                return true;
        }
    }
}