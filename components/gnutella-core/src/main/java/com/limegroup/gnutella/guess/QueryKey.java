package com.limegroup.gnutella.guess;

import com.sun.java.util.collections.*;
import java.io.*;
import org.logi.crypto.keys.DESKey;
import com.limegroup.gnutella.ByteOrder;
import java.net.InetAddress;

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
 * getQueryKey(ip, port, ....) and the two Secret inner classes (SecretKey and
 * SecretPad).
 */
public class QueryKey {

    /** As detailed by the GUESS spec.
     */
    public final int MIN_QK_SIZE_IN_BYTES = 4;
    /** As detailed by the GUESS spec.
     */
    public final int MAX_QK_SIZE_IN_BYTES = 16;

    /** The Query Key.  MIN_QK_SIZE_IN_BYTES <=_queryKey.length <=
     *  MAX_QK_SIZE_IN_BYTES
     */
    private byte[] _queryKey;

    static {
        // initialize the logi.crypto package
        org.logi.crypto.Crypto.initRandom();
    }
    
    private QueryKey(byte[] key) throws IllegalArgumentException {
        if ((key.length < MIN_QK_SIZE_IN_BYTES) ||
            (key.length > MAX_QK_SIZE_IN_BYTES)
            )
            throw new IllegalArgumentException();
        _queryKey = new byte[key.length];
        System.arraycopy(key, 0, _queryKey, 0, key.length);
    }


    public boolean equals(Object o) {
        if (!(o instanceof QueryKey))
            return false;
        QueryKey other = (QueryKey) o;
        return Arrays.equals(_queryKey, other._queryKey);
    }

    // NOT A VERY GOOD HASH FUNCTION RIGHT NOW - NO BIGGIE FOR NOW....
    // TODO: make a better hash function
    public int hashCode() {
        int retInt = 0;
        for (int i = 0; i < 4; i++) {
            int index = _queryKey[i]%_queryKey.length;
            if (index < 0)
                index *= -1;
            retInt += _queryKey[index] * 7;
        }
        return retInt;
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


    /** Use this method to construct Query Keys that you get from network
     *  commerce.
     */    
    public static QueryKey getQueryKey(byte[] networkQK) 
        throws IllegalArgumentException {
        return new QueryKey(networkQK);
    }
     
    /** Returns a new SecretKey to be used in generation of QueryKeys.
     */   
    public static SecretKey generateSecretKey() {
        return new SecretKey();
    }

    /** Returns a new SecretPad to be used in generation of QueryKeys.
     */
    public static SecretPad generateSecretPad() {
        return new SecretPad();
    }

    /** Generates a QueryKey for a given IP:Port combo.
     *  For a given IP:Port combo, using a different SecretKey and/or SecretPad
     *  will result in a different QueryKey.
     *  @param secretKey The SecretKey to be used in the generation.
     *  @param secretPad The SecretPad to be used in the generation.
     */
    public static QueryKey getQueryKey(InetAddress ip, int port,
                                       SecretKey secretKey, 
                                       SecretPad secretPad) {
        byte[] toEncrypt = new byte[8];
        // get all the input bytes....
        byte[] ipBytes = ip.getAddress();
        short shortPort = (short) port;
        byte[] portBytes = new byte[2];
        ByteOrder.short2leb(shortPort, portBytes, 0);
        // dynamically set where the secret pad will be....
        int first, second;
        first = secretPad._pad[0] % 8;
        if (first < 0)
            first *= -1;
        second = secretPad._pad[1] % 8;
        if (second < 0)
            second *= -1;
        if (second == first) {
            if (first == 0)
                second = 1;
            else 
                second = first - 1;
        }
        // put everything in toEncrypt
        toEncrypt[first] = secretPad._pad[0];
        toEncrypt[second] = secretPad._pad[1];
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
        secretKey._DESKey.encrypt(toEncrypt, 0, encrypted, 0);
        return new QueryKey(encrypted);
    }

    //--------------------------------------


    //--------------------------------------
    //--- PUBLIC INNER CLASSES
    
    /**The Key used in generating a QueryKey.  Needed to get a derive a
     * QueryKey from a IP:Port combo.
     */
    public static class SecretKey {
        // the implementation of the SecretKey - users don't need to know about
        // it
        private DESKey _DESKey;
        private SecretKey() {
            _DESKey = new DESKey();
        }
    }

    /**Depending on the algorithm, this may be needed to derive a QueryKey (in
     * addition to a SecretKey).
     */
    public static class SecretPad {
        // for DES, we need a 2-byte pad, since the IP:Port combo is 6 bytes.
        private byte[] _pad;
        private SecretPad() {
            _pad = new byte[2];
            (new Random()).nextBytes(_pad);
        }
    }


    //--------------------------------------

}
