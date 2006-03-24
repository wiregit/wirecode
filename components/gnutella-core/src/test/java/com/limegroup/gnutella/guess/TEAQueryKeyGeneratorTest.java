package com.limegroup.gnutella.guess;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.math.BigInteger;
import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;

public class TEAQueryKeyGeneratorTest extends BaseTestCase {

    public TEAQueryKeyGeneratorTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(TEAQueryKeyGeneratorTest.class);
    }
    
    // Generates 420 QueryKeys using random secret keys 
    // and makes sure none of them contain 0x00 or 0x1C
    public void testAllBytesNetworkSafe() throws Exception {
        Random rand = new Random();
        InetAddress[] addresses = new InetAddress[20];
        byte[] randAddressBytes = new byte[4];
        for(int i=addresses.length-1; i >= 0; --i) {
            rand.nextBytes(randAddressBytes);
            addresses[i] = InetAddress.getByAddress(randAddressBytes);
        }
        
        for(int i=20; i > 0; --i) {
            TEAQueryKeyGenerator key = new TEAQueryKeyGenerator();
            for(int j=addresses.length-1; j >= 0; --j) {
                byte[] keyBytes = key.getKeyBytes(addresses[j], 1024 + i);
                for(int k=keyBytes.length-1; k >= 0; --k) {
                    int keyByte = keyBytes[k];
                    if (keyByte == 0x00 || keyByte == 0x1C) {
                        byte[] printBuf = new byte[keyBytes.length+1];
                        System.arraycopy(keyBytes, 0, printBuf, 1, keyBytes.length);
                        printBuf[0] = (byte) 1;
                        fail("keyBytes contains illegal byte "+ 
                                (new BigInteger(1,printBuf)).toString(16));
                    }
                }
            }
        }
    }
    
    //  Generates 420 QueryKeys using random secret keys 
    // and makes sure that they all can be verified by
    // the QueryKeyGenerator that created them
    public void testKeyVerification() throws Exception {
        Random rand = new Random();
        InetAddress[] addresses = new InetAddress[20];
        byte[] randAddressBytes = new byte[4];
        for(int i=addresses.length-1; i >= 0; --i) {
            rand.nextBytes(randAddressBytes);
            addresses[i] = InetAddress.getByAddress(randAddressBytes);
        }
        
        for(int i=20; i > 0; --i) {
            TEAQueryKeyGenerator key = new TEAQueryKeyGenerator();
            for(int j=addresses.length-1; j >= 0; --j) {
                byte[] keyBytes = key.getKeyBytes(addresses[j], 1024 + i);
                if (! key.checkKeyBytes(keyBytes, addresses[j], 1024 + i)) {
                    byte[] printBuf = new byte[keyBytes.length+1];
                    System.arraycopy(keyBytes, 0, printBuf, 1, keyBytes.length);
                    printBuf[0] = (byte) 1;
                    fail("keyBytes fails to verify "+ 
                            (new BigInteger(1,printBuf)).toString(16));
                }
            }
        }
    }
    
    
    // Breaks abstraction, but ensures that TEA is implemented correctly
    public void testTEAtestVectors() throws Exception {
        TEAVectorTester key = new TEAVectorTester(0,0,0,0,0,0);
        long cipherBlock = key.publicEncrypt(0x0L);
        assertEquals("TEA test vecotr failed", 0x41EA3A0A94BAA940L, cipherBlock);
        assertEquals("TEA test vector failed.", 0x41EA3A0A, key.encrypt(0,0));
        
        // get at the other half of the block and test the 64-bit rotation code
        key = new TEAVectorTester(0,0,0,0,0,32); 
        assertEquals("TEA test vector failed.", 0x94BAA940, key.encrypt(0,0));
    }
    
    private class TEAVectorTester extends TEAQueryKeyGenerator {
        /** Set up a tester with given TEA encryption keys */
        public TEAVectorTester(int k0, int k1, int k2, int k3, 
                int preRotate, int postRotate) {
            super(k0,k1,k2,k3, preRotate, postRotate);
        }
        
        /** In the absence of 0x00 and 0x1C bytes, returns the
         * left int of the TEA encryption block after encrypting (left,right).
         * Use the postRotate to get at different parts of the TEA encryption
         * output.
         */
        public int encrypt(int left, int right) throws UnknownHostException {
            // Prepare right for the unusual encoding
            // of IP addresses as ints used in QueryKeyGenerator
            for(int i=0x80; i>0; i <<= 8) {
                if ((right & i) != 0) {
                    right = (~right) ^ (i-1) ^ i;
                }
            }
            byte[] ipBytes = new byte[4];
            for(int i=0; i <= 3; ++i) {
                ipBytes[i] = (byte) right;
                right >>>= 8;
            }
            
            byte[] resultBytes = getKeyBytes(InetAddress.getByAddress(ipBytes), left);
            
            int byteCount = resultBytes.length;
            int result = 0;
            for(int i=byteCount-4; i<byteCount ; ++i) {
                result <<= 8;
                result |= 0xFF & resultBytes[i];
            }
            
            return result;
        }
        
        // Accesses the private method super.encrypt(long)
        public long publicEncrypt(long arg)
            throws NoSuchMethodException, IllegalAccessException,
            java.lang.reflect.InvocationTargetException {
            Long result = (Long) PrivilegedAccessor.invokeMethod(this, "encrypt", 
                                     new Object[] { new Long(arg)},
                                     new Class[] { long.class });
            return result.longValue();
        }
    }
    
}
