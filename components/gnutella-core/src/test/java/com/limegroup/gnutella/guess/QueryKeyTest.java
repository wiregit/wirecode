package com.limegroup.gnutella.guess;

import junit.framework.*;
import java.util.*;
import java.net.*;
import com.limegroup.gnutella.util.*;

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
        try {
            ip = InetAddress.getByName("10.254.0.42");
        }
        catch (Exception ignored) {
            assertTrue(false);
        }
        qk1 = QueryKey.getQueryKey(ip, port, key, pad);
        qk2 = QueryKey.getQueryKey(ip, port, key, pad);
        assertTrue(qk1.equals(qk2));
        try {
            ip = InetAddress.getByName("127.0.0.1");
        }
        catch (Exception ignored) {
            assertTrue(false);
        }
        qk1 = QueryKey.getQueryKey(ip, port, key, pad);
        qk2 = QueryKey.getQueryKey(ip, port, key, pad);
        assertTrue(qk1.equals(qk2));
    }

    public void testSamePadModulo() {
        QueryKey.SecretKey key = QueryKey.generateSecretKey();
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName("www.limewire.com");
        }
        catch (Exception ignored) {
            assertTrue(false);
        }
        int port = 6346;
        // suppose the pads have the same modulo 8 - this case should be
        // handled.
        try {
            QueryKey.SecretPad pad = QueryKey.generateSecretPad();
            byte[] innards = (byte[]) PrivilegedAccessor.getValue(pad, "_pad");
            // test lower bound
            innards[0] = 0;
            innards[1] = 0;
            QueryKey qk1 = QueryKey.getQueryKey(ip, port, key, pad);
            QueryKey qk2 = QueryKey.getQueryKey(ip, port, key, pad);
            assertTrue(qk1.equals(qk2));
            // test everything else bound
            innards[0] = 1;
            innards[1] = 1;
            qk1 = QueryKey.getQueryKey(ip, port, key, pad);
            qk2 = QueryKey.getQueryKey(ip, port, key, pad);
            assertTrue(qk1.equals(qk2));
        }
        catch (Exception ignored) {
            assertTrue(false);
        }
    }

    public void testNegativePad() {
        QueryKey.SecretKey key = QueryKey.generateSecretKey();
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName("www.limewire.com");
        }
        catch (Exception ignored) {
            assertTrue(false);
        }
        int port = 6346;
        // suppose the pads have the same modulo 8 - this case should be
        // handled.
        try {
            QueryKey.SecretPad pad = QueryKey.generateSecretPad();
            byte[] innards = (byte[]) PrivilegedAccessor.getValue(pad, "_pad");
            // test first negative
            innards[0] = -1;
            innards[1] = 0;
            QueryKey qk1 = QueryKey.getQueryKey(ip, port, key, pad);
            QueryKey qk2 = QueryKey.getQueryKey(ip, port, key, pad);
            assertTrue(qk1.equals(qk2));
            // test secind negative
            innards[0] = 5;
            innards[1] = -24;
            qk1 = QueryKey.getQueryKey(ip, port, key, pad);
            qk2 = QueryKey.getQueryKey(ip, port, key, pad);
            assertTrue(qk1.equals(qk2));
            // test everything negative
            innards[0] = -41;
            innards[1] = -51;
            qk1 = QueryKey.getQueryKey(ip, port, key, pad);
            qk2 = QueryKey.getQueryKey(ip, port, key, pad);
            assertTrue(qk1.equals(qk2));
        }
        catch (Exception ignored) {
            assertTrue(false);
        }
    }


}
