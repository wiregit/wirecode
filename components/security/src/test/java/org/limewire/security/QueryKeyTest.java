package org.limewire.security;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

import org.limewire.security.QueryKey;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;


public class QueryKeyTest extends BaseTestCase {
    public QueryKeyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(QueryKeyTest.class);
    }

    public void testConstruction() throws Exception {
        try {
            new QueryKey(new byte[3]);
            fail("exception should have been thrown.");
        }
        catch (IllegalArgumentException ignored) {}
        try {
            new QueryKey(new byte[17]);
            fail("exception should have been thrown.");
        }
        catch (IllegalArgumentException ignored) {}
        
        byte[] qk = new byte[8];
        Random rand = new Random();
        Arrays.sort(qk);
        while (Arrays.binarySearch(qk, (byte) 0x1c) < 0) {
            rand.nextBytes(qk);
            Arrays.sort(qk);
        }
        QueryKey key1 = null, key2 = null;
        key1 = new QueryKey(qk);
        key2 = new QueryKey(qk);
        assertEquals(key1,key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        
        byte []qk3 = new byte[8];
        rand.nextBytes(qk3);
        QueryKey key3 = new QueryKey(qk3);
        assertNotEquals(key1, key3);
        assertNotEquals(key2, key3);
        assertEquals(key3, key3);
        // don't test key1.hashCode() vs. key3.hashCode() - they may very easily
        // conflict
    }

    public void testSimpleGeneration() throws Exception {
        InetAddress ip = null;
        ip = InetAddress.getByName("www.limewire.com");
        int port = 6346;
        AbstractQueryKey qk1 = new QueryKey(ip, port);
        AbstractQueryKey qk2 = new QueryKey(ip, port);
        assertEquals(qk1,qk2);
        ip = InetAddress.getByName("10.254.0.42");
        qk1 = new QueryKey(ip, port);
        qk2 = new QueryKey(ip, port);
        assertEquals(qk1,qk2);
        ip = InetAddress.getByName("127.0.0.1");
        qk1 = new QueryKey(ip, port);
        qk2 = new QueryKey(ip, port);
        assertEquals(qk1,qk2);
    }

    public void testQueryKeys() throws Exception {
        InetAddress addr1 = InetAddress.getByName("www.limewire.com");
        InetAddress addr2 = InetAddress.getByName("www.microsoft.com");
        
        int port1 = 1234;
        int port2 = 4321;
        
        QueryKey key1 = new QueryKey(addr1, port1);
        QueryKey key2 = new QueryKey(addr1, port2);
        QueryKey key3 = new QueryKey(addr2, port1);
        
        assertTrue(key1.isFor(addr1, port1));
        assertFalse(key1.equals(key2));
        assertFalse(key1.equals(key3));
        assertFalse(key1.isFor(addr1, port2));
        assertFalse(key1.isFor(addr2, port1));
        assertFalse(key1.isFor(addr2, port2));
        
        assertTrue(key2.isFor(addr1, port2));
        assertFalse(key2.equals(key1));
        assertFalse(key2.equals(key3));
        assertFalse(key2.isFor(addr1, port1));
        assertFalse(key2.isFor(addr2, port1));
        assertFalse(key2.isFor(addr2, port2));
        
        assertTrue(key3.isFor(addr2, port1));
        assertFalse(key3.equals(key1));
        assertFalse(key3.equals(key2));
        assertFalse(key3.isFor(addr1, port1));
        assertFalse(key3.isFor(addr1, port2));
        assertFalse(key3.isFor(addr2, port2));
    }
    


    public void testOddsAndEnds() throws Exception {
        // test to make clover 100% for QK
        AbstractQueryKey qk = new QueryKey(InetAddress.getLocalHost(), 6346);
        assertFalse(qk.equals(new Object()));
        qk.toString();
    }
    
}
