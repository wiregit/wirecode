package com.limegroup.gnutella.guess;

import junit.framework.*;
import java.util.*;
import java.net.*;

public class QueryKeyTest extends TestCase {
    public QueryKeyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(QueryKeyTest.class);
    }

    public void testConstruction() {
        try {
            QueryKey.getQueryKey(new byte[3]);
            assertTrue(false);
        }
        catch (IllegalArgumentException ignored) {}
        try {
            QueryKey.getQueryKey(new byte[17]);
            assertTrue(false);
        }
        catch (IllegalArgumentException ignored) {}
        
        byte[] qk = new byte[8];
        (new Random()).nextBytes(qk);
        QueryKey key1 = null, key2 = null;
        try {
            key1 = QueryKey.getQueryKey(qk);
            key2 = QueryKey.getQueryKey(qk);
        }
        catch (IllegalArgumentException ignored) {
            assertTrue(false);
        }
        assertTrue(key1.equals(key2));
        assertTrue(key1.hashCode() == key2.hashCode());
    }

    public void testSimpleGeneration() {
        QueryKey.SecretKey key = QueryKey.generateSecretKey();
        QueryKey.SecretPad pad = QueryKey.generateSecretPad();
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName("www.limewire.com");
        }
        catch (Exception ignored) {
            assertTrue(false);
        }
        int port = 6346;
        QueryKey qk1 = QueryKey.getQueryKey(ip, port, key, pad);
        QueryKey qk2 = QueryKey.getQueryKey(ip, port, key, pad);
        assertTrue(qk1.equals(qk2));
    }

}
