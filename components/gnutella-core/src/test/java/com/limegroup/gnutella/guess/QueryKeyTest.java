package com.limegroup.gnutella.guess;

import junit.framework.*;
import java.util.*;
import java.net.*;
import java.io.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;

public class QueryKeyTest extends com.limegroup.gnutella.util.BaseTestCase {
    public QueryKeyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(QueryKeyTest.class);
    }

    public void testConstruction() throws Exception {
        try {
            QueryKey.getQueryKey(new byte[3], false);
            fail("exception should have been thrown.");
        }
        catch (IllegalArgumentException ignored) {}
        try {
            QueryKey.getQueryKey(new byte[17], false);
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
        QueryKey key1 = null, key2 = null, key3 = null;
        key1 = QueryKey.getQueryKey(qk, true);
        key2 = QueryKey.getQueryKey(qk, true);
        key3 = QueryKey.getQueryKey(qk, false);
        assertEquals(key1,key2);
        assertNotEquals(key1,key3);
        assertEquals(key1.hashCode(), key2.hashCode());
        // don't test key1.hashCode() vs. key3.hashCode() - they may very easily
        // conflict
    }

    public void testSimpleGeneration() throws Exception {
        QueryKey.SecretKey key = QueryKey.generateSecretKey();
        QueryKey.SecretPad pad = QueryKey.generateSecretPad();
        InetAddress ip = null;
        ip = InetAddress.getByName("www.limewire.com");
        int port = 6346;
        QueryKey qk1 = QueryKey.getQueryKey(ip, port, key, pad);
        QueryKey qk2 = QueryKey.getQueryKey(ip, port, key, pad);
        assertEquals(qk1,qk2);
        ip = InetAddress.getByName("10.254.0.42");
        qk1 = QueryKey.getQueryKey(ip, port, key, pad);
        qk2 = QueryKey.getQueryKey(ip, port, key, pad);
        assertEquals(qk1,qk2);
        ip = InetAddress.getByName("127.0.0.1");
        qk1 = QueryKey.getQueryKey(ip, port, key, pad);
        qk2 = QueryKey.getQueryKey(ip, port, key, pad);
        assertEquals(qk1,qk2);
    }

    public void testSamePadModulo() throws Exception {
        QueryKey.SecretKey key = QueryKey.generateSecretKey();
        InetAddress ip = null;
        ip = InetAddress.getByName("www.limewire.com");
        int port = 6346;
        // suppose the pads have the same modulo 8 - this case should be
        // handled.
        QueryKey.SecretPad pad = QueryKey.generateSecretPad();
        byte[] innards = (byte[]) PrivilegedAccessor.getValue(pad, "_pad");
        // test lower bound
        innards[0] = 0;
        innards[1] = 0;
        QueryKey qk1 = QueryKey.getQueryKey(ip, port, key, pad);
        QueryKey qk2 = QueryKey.getQueryKey(ip, port, key, pad);
        assertEquals(qk1,qk2);
        // test everything else bound
        innards[0] = 1;
        innards[1] = 1;
        qk1 = QueryKey.getQueryKey(ip, port, key, pad);
        qk2 = QueryKey.getQueryKey(ip, port, key, pad);
        assertEquals(qk1,qk2);
    }

    public void testNegativePad() throws Exception {
        QueryKey.SecretKey key = QueryKey.generateSecretKey();
        InetAddress ip = null;
        ip = InetAddress.getByName("www.limewire.com");
        int port = 6346;
        // suppose the pads have the same modulo 8 - this case should be
        // handled.
        QueryKey.SecretPad pad = QueryKey.generateSecretPad();
        byte[] innards = (byte[]) PrivilegedAccessor.getValue(pad, "_pad");
        // test first negative
        innards[0] = -1;
        innards[1] = 0;
        QueryKey qk1 = QueryKey.getQueryKey(ip, port, key, pad);
        QueryKey qk2 = QueryKey.getQueryKey(ip, port, key, pad);
        assertEquals(qk1,qk2);
        // test secind negative
        innards[0] = 5;
        innards[1] = -24;
        qk1 = QueryKey.getQueryKey(ip, port, key, pad);
        qk2 = QueryKey.getQueryKey(ip, port, key, pad);
        assertEquals(qk1,qk2);
        // test everything negative
        innards[0] = -41;
        innards[1] = -51;
        qk1 = QueryKey.getQueryKey(ip, port, key, pad);
        qk2 = QueryKey.getQueryKey(ip, port, key, pad);
        assertEquals(qk1,qk2);
    }


    // Makes sure QueryKeys have no problem going in and out of GGEP blocks
    public void testQueryKeysAndGGEP() throws Exception {
        Random rand = new Random();
        for (int i = 4; i < 17; i++) {
            byte[] qk = new byte[i];
            Arrays.sort(qk);
            // make sure the bytes have offensive characters....
            while ((Arrays.binarySearch(qk, (byte) 0x1c) < 0) ||
                   (Arrays.binarySearch(qk, (byte) 0x00) < 0)) {
                rand.nextBytes(qk);
                Arrays.sort(qk);
            }
            QueryKey queryKey = QueryKey.getQueryKey(qk, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            queryKey.write(baos);
            GGEP in = new GGEP(false);
            in.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT,
                   baos.toByteArray());
            baos = new ByteArrayOutputStream();
            in.write(baos);
            GGEP out = new GGEP(baos.toByteArray(), 0, null);
            QueryKey queryKey2 = 
            QueryKey.getQueryKey(out.getBytes(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT), false);
            assertEquals("qks not equal, i = " + i,
                       queryKey, queryKey2);
        }
    }

}
