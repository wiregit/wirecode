package com.limegroup.gnutella.guess;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

import org.logi.crypto.keys.DESKey;

import com.limegroup.gnutella.ByteOrder;

/**
 * Abstraction for a Query Key as detailed in the GUESS protocol spec.
 * Provides:
 * - encapsulation of (all, LW and non-LW) Query Keys
 * - generation of Query Keys (hence, it contains the LimeWire QK Algorithm)
 *
 * A Query Key is a credential necessary to perform a GUESS Query.  A Query Key
 * instance is immutable.
 *
 * If you want to change the underlying generation algorithm, you need to change
 * getKeyBytes(ip, port) and of the inner class QueryKeyGenerator.
 */
public final class QueryKey {

    /**
     * Constant for the <tt>QueryKeyGenerator</tt> holding our secret key(s).
     */
    private static QueryKeyGenerator secretKey = null;
    
    /** As detailed by the GUESS spec.
     */
    public static final int MIN_QK_SIZE_IN_BYTES = 4;
    /** As detailed by the GUESS spec.
     */
    public static final int MAX_QK_SIZE_IN_BYTES = 16;

    /** The Query Key.  MIN_QK_SIZE_IN_BYTES <=_queryKey.length <=
     *  MAX_QK_SIZE_IN_BYTES
     */
    private byte[] _queryKey;
    
    /**
     * Cached value to make hashCode() much faster.
     */
    private final int _hashCode;

    static {
        // initialize the logi.crypto package
        org.logi.crypto.Crypto.initRandom();
        secretKey = new QueryKeyGenerator();
    }
    
    private QueryKey(byte[] key) throws IllegalArgumentException {
        if(!isValidQueryKeyBytes(key))
            throw new IllegalArgumentException();
        _queryKey = new byte[key.length];
        System.arraycopy(key, 0, _queryKey, 0, key.length);
        
        // While we have key in the CPU data cache, calculate _hashCode
        int code = 0x5A5A5A5A;
        // Mix all bits of key fairly evenly into code
        for (int i = key.length - 1; i >= 0; --i) {
            code ^= (0xFF & key[i]);
            // One-to-one mixing function from RC6 cipher
            long codeLong = 0xFFFFFFFFL & code;
            code = (int) (codeLong * ((codeLong << 1) + 1));
            // Left circular rotate code by 5 bits
            code = (code >>> 27) | (code << 5);
        }
        _hashCode = code;
    }

     /** Returns a new QueryKeyGenerator with random secret key(s).
      */
     public static QueryKeyGenerator createKeyGenerator() {
         return new QueryKeyGenerator();
     }

    public boolean equals(Object o) {
        if (o.hashCode() != _hashCode)
            return false;
        if (!(o instanceof QueryKey))
            return false;
        QueryKey other = (QueryKey) o;
        return Arrays.equals(_queryKey, other._queryKey);
    }

    public int hashCode() {
       return _hashCode;
    }

    public void write(OutputStream out) throws IOException {
        out.write(_queryKey);
    }

    /** Returns a String with the QueryKey represented as a BigInteger.
     */
    public String toString() {
        return "{Query Key: " + (new java.math.BigInteger(_queryKey)) + "}";
    }

    //--------------------------------------
    //--- PUBLIC STATIC CONSTRUCTION METHODS

    /**
     * Determines if the bytes are valid for a qkey.
     */
    public static boolean isValidQueryKeyBytes(byte[] key) {
        return key != null &&
               key.length >= MIN_QK_SIZE_IN_BYTES &&
               key.length <= MAX_QK_SIZE_IN_BYTES;
    }


    /** Use this method to construct Query Keys that you get from network
     *  commerce.  If you are using this for testing purposes, be aware that
     *  QueryKey in QueryRequests cannot contain the GEM extension delimiter 
     *  0x1c or nulls, so send true as the second param...
     *  
     *  @param networkQK the bytes you want to make a QueryKey.
     *  @param prepareForNet true to prepare the QueryKey for net transport.
     */    
    public static QueryKey getQueryKey(byte[] networkQK, boolean prepareForNet) 
        throws IllegalArgumentException {
        if (prepareForNet)  {
            boolean alreadyCopied = false;
            for (int i = networkQK.length - 1; i >= 0; --i) {
                int nextByte = networkQK[i];
                // The old prepareForNetwork() seemed to leave cobbs encoding to get
                // of nulls?  TODO: is it okay to leave nulls alone?
                if (nextByte == 0x1c) {
                    if (! alreadyCopied) {
                        networkQK = (byte[]) networkQK.clone();
                    }
                    networkQK[i] = (byte) (0xFA);
                }
            }
        }
        return new QueryKey(networkQK);
    }

    /** Generates a QueryKey for a given IP:Port combo.
     *  For a given IP:Port combo, using a different SecretKey and/or SecretPad
     *  will result in a different QueryKey.  The return value is constructed
     *  with prepareForNet equal to true.
     *  
     * @param ip the IP address of the other node
     * @param port the port of the other node
     */
    public static QueryKey getQueryKey(InetAddress ip, int port) {
        return getQueryKey(secretKey.getKeyBytes(ip,port), true);
    }

    /** Generates a QueryKey for a given IP:Port combo.
     *  For a given IP:Port combo, using a different QueryKeyGenerator
     *  will result in a different QueryKey.  The instance method
     *  prepareForNetwork() is called prior to returning the QueryKey.
     * @param ip the IP address of the other node
     * @param port the port of the other node
     */
    public static QueryKey getQueryKey(InetAddress ip, int port,
                                       QueryKeyGenerator keyGen) {
        return getQueryKey(keyGen.getKeyBytes(ip, port), true);
    }

    //--------------------------------------


    //--------------------------------------
    //--- PUBLIC INNER CLASSES
    
    /**The algorithm and secret key(s) used in generating a QueryKey.
     * This is needed to create a QueryKey from a IP:Port combo.
     */
    public static class QueryKeyGenerator {
        // the implementation of the SecretKey - users don't need to know about
        // it
        private final DESKey _DESKey;
        // for DES, we need a 2-byte pad, since the IP:Port combo is 6 bytes.
        private final byte[] _pad;
        private QueryKeyGenerator() {
            _DESKey = new DESKey();
            _pad = new byte[2];
            (new Random()).nextBytes(_pad);
        }
        
        /** Returns the raw bytes for a QueryKey, which may need to
         * be processed to remove 0x1C and 0x00 before sending on
         * the network.
         */
        public byte[] getKeyBytes(InetAddress ip, int port) {
            byte[] toEncrypt = new byte[8];
            // get all the input bytes....
            byte[] ipBytes = ip.getAddress();
            short shortPort = (short) port;
            byte[] portBytes = new byte[2];
            ByteOrder.short2leb(shortPort, portBytes, 0);
            // dynamically set where the secret pad will be....
            int first, second;
            first = _pad[0] % 8;
            if (first < 0)
                first *= -1;
            second = _pad[1] % 8;
            if (second < 0)
                second *= -1;
            if (second == first) {
                if (first == 0)
                    second = 1;
                else
                    second = first - 1;
            }
            // put everything in toEncrypt
            toEncrypt[first] = _pad[0];
            toEncrypt[second] = _pad[1];
            int j = 0;
            for (int i = 0; i < 4; i++) {
                while ((j == first) || (j == second))
                    j++;
                toEncrypt[j++] = ipBytes[i];
            }
            for (int i = 0; i < 2; i++) {
                while ((j == first) || (j == second))
                    j++;
                toEncrypt[j++] = portBytes[i];
            }
            // encrypt that bad boy!
            byte[] encrypted = new byte[8];
            synchronized (_DESKey) {
                _DESKey.encrypt(toEncrypt, 0, encrypted, 0);
            }
            return encrypted;
        }
    }
    //--------------------------------------

}
