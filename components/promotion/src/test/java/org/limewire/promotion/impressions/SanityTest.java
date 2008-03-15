package org.limewire.promotion.impressions;

import java.util.Date;

import org.apache.commons.httpclient.util.Base64;
import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtil;

import junit.framework.Test;

public class SanityTest extends BaseTestCase {

    public SanityTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SanityTest.class);
    }
    
    public void testNumbers() {
        String encoded = "AHuXyiIAAAAcqrXDsw==";
        byte[] encodedBytes = encoded.getBytes();
        byte[] decodedBytes = Base64.decode(encodedBytes);
                
        byte n = decodedBytes[0];
        System.out.println(n);
    }
    
    public void testFourNumbers() {
        String encoded = "AAAAHKq1w7NwfCqH8iSwAAAAAAAAAAAAAAAAHKq1w7M=";
        byte[] encodedBytes = encoded.getBytes();
        byte[] decodedBytes = Base64.decode(encodedBytes);
        
        System.out.println("length:" + decodedBytes.length);
        
        byte[][] bbs = new byte[4][];
        long[] ls = new long[bbs.length];
        for (int i=0; i<ls.length; i++) {
            byte[] bs = new byte[8];
            bbs[i] = bs;
            System.arraycopy(decodedBytes, i*8, bs, 0, 8);
            ls[i] = ByteUtil.toLongFromBytes(bs);
        }
        
        for (int i=0; i<ls.length; i++) {
            System.out.print("{ ");
            for (byte b : bbs[i]) System.out.print(b + " ");
            System.out.print("{}");
            System.out.print(ls[i]);
            System.out.println();
        }
    }

    public void testSanity() {
        long l = new Date().getTime();
        byte[] bytes = ByteUtil.convertToBytes(l, 8);
        byte[] encoded = Base64.encode(bytes);
        byte[] decoded = Base64.decode(encoded);
        
        for (int i=0; i<bytes.length; i++) {
            assertEquals(bytes[i],decoded[i]);
        }
        
        System.out.println("value   :" + l);
        System.out.println("bytes   :" + new String(bytes));
        System.out.println("encoded :" + new String(encoded));
        System.out.println("decoded :" + new String(decoded));
    }
    
    public void testSanity2() {
        byte[] encoded = "AAAAHKqlw7M=".getBytes();
        byte[] decoded = Base64.decode(encoded);
        long l = ByteUtil.toLongFromBytes(decoded);
        long l2 = ByteUtil.toLongFromBytes(decoded);
        assertEquals(l,l2);
    }  
    
    public void testSanity3() {
        long l = new Date().getTime();
        byte[] encoded = Base64.encode(ByteUtil.convertToBytes(l, 8));
        byte[] decoded = Base64.decode(encoded);
        long l2 = ByteUtil.toLongFromBytes(decoded);
        assertEquals(l,l2);
    }      
    
}
