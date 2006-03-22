package com.limegroup.gnutella.guess;

import java.io.ByteArrayOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;

import junit.framework.Test;

import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.util.PrivilegedAccessor;

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
        QueryKeyGenerator secretKey = QueryKey.createKeyGenerator();
        InetAddress ip = null;
        ip = InetAddress.getByName("www.limewire.com");
        int port = 6346;
        QueryKey qk1 = QueryKey.getQueryKey(ip, port, secretKey);
        QueryKey qk2 = QueryKey.getQueryKey(ip, port, secretKey);
        assertEquals(qk1,qk2);
        ip = InetAddress.getByName("10.254.0.42");
        qk1 = QueryKey.getQueryKey(ip, port, secretKey);
        qk2 = QueryKey.getQueryKey(ip, port, secretKey);
        assertEquals(qk1,qk2);
        ip = InetAddress.getByName("127.0.0.1");
        qk1 = QueryKey.getQueryKey(ip, port, secretKey);
        qk2 = QueryKey.getQueryKey(ip, port, secretKey);
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


    public void testOddsAndEnds() throws Exception {
        // test to make clover 100% for QK
        QueryKey qk = QueryKey.getQueryKey(InetAddress.getLocalHost(), 6346);
        assertFalse(qk.equals(new Object()));
        qk.toString();
    }
    
}
