package org.limewire.security;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.SimpleTimer;

/**
 * Abstraction for a Query Key as detailed in the GUESS protocol spec.
 * Provides:
 * - encapsulation of (all, LW and non-LW) Query Keys
 * - generation of Query Keys (hence, it contains the LimeWire QK Algorithm)
 *
 * A Query Key is a credential necessary to perform a GUESS Query.  A Query Key
 * instance is immutable.
 *
 * QueryKeys make spoofing UDP IP addresses about as difficult as spoofing TCP
 * IP addresses by forcing some two-way communication before the heavy data
 * transfer occurs (sending search results).  This prevents the use of the
 * Gnutella network as a huge DDoS botnet.
 *
 * If you want to change the underlying generation algorithm, you need to create
 * a new class that implements QueryKeyGenerator and modify getQueryKey(InetAddress, int)
 * to use your new QueryKeyGenerator implementation. 
 */
public final class QueryKey implements SecurityToken {

    private static final Log LOG = LogFactory.getLog(QueryKey.class);
    
    /**
     * The keychain with the private keys.
     */
    private static volatile QKGeneratorKeychain keychain = new SimpleKeychain();
    
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

    private QueryKey(byte[] key, boolean prepareForNet) throws IllegalArgumentException {
        if(!isValidQueryKeyBytes(key))
            throw new IllegalArgumentException();
        key = key.clone();
        
        if (prepareForNet)  {
            for (int i = key.length - 1; i >= 0; --i) {
                // The old prepareForNetwork() seemed to leave cobbs encoding to get
                // of nulls?  TODO: is it okay to leave nulls alone?
                if (key[i] == 0x1c) {
                    key[i] = (byte) (0xFA);
                }
            }
        }
        
        // TODO: Can't we use Arrays.hashCode(byte[]) ???
        // While we have key in the CPU data cache, calculate _hashCode
        int code = 0x5A5A5A5A;
        // Mix all bits of key fairly evenly into code
        for (int i = key.length - 1; i >= 0; --i) {
            code ^= (0xFF & key[i]);
            // One-to-one mixing function from RC6 cipher:  
            // f(x) = (2*x*x + x) mod 2**N
            // We only care about the low-order 32-bits, so there's no
            // need to use longs to emulate 32-bit unsigned multiply.
            code = (code * ((code << 1) + 1));
            // Left circular shift (rotate) code by 5 bits
            code = (code >>> 27) | (code << 5);
        }
        
        _queryKey = key;
        _hashCode = code;
    }
    
    public static void setSettingsProvider(SettingsProvider provider) {
        keychain = new QKGeneratorRotator(SimpleTimer.sharedTimer(),
                new TEAFactory(),
                provider);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.security.SecurityToken#isFor(java.net.SocketAddress)
     */
    public boolean isFor(SocketAddress address) {
        InetAddress ip = ((InetSocketAddress)address).getAddress();
        int port = ((InetSocketAddress)address).getPort();
        return isFor(ip, port);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.security.SecurityToken#isFor(java.net.InetAddress, int)
     */
    public boolean isFor(InetAddress ip, int port) {
        for (QueryKeyGenerator validKey : keychain.getValidQueryKeyGenerators()) {
            if (validKey.checkKeyBytes(_queryKey,ip,port)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("QueryKey valid:"+this);
                }
                return true;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("QueryKey corrupt or expired:"+this);
        }
        return false;
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

    /*
     * (non-Javadoc)
     * @see org.limewire.security.SecurityToken#write(java.io.OutputStream)
     */
    public void write(OutputStream out) throws IOException {
        out.write(_queryKey);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.security.SecurityToken#getTokenLength()
     */
    public int getTokenLength() {
        return _queryKey.length;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.security.SecurityToken#getBytes()
     */
    public byte[] getBytes() {
        byte[] b = new byte[_queryKey.length];
        System.arraycopy(_queryKey, 0, b, 0, _queryKey.length);
        return b;
    }
    
    /** Returns a String with the QueryKey represented in hexadecimal.
     */
    public String toString() {
        return "{Query Key: " + (new BigInteger(1, _queryKey)).toString(16) + "}";
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
     *  
     *  @param networkQK the bytes you want to make a QueryKey.
     *  @param prepareForNet true to prepare the QueryKey for net transport.
     */    
    public static QueryKey getQueryKey(byte[] networkQK, boolean prepareForNet) 
            throws IllegalArgumentException {
        return new QueryKey(networkQK, prepareForNet);
    }

    /** Generates a QueryKey for a given SocketAddress.
     *  For a given SocketAddress, using a different SecretKey and/or SecretPad
     *  will result in a different QueryKey.  The return value is constructed
     *  with prepareForNet equal to true.
     *  
     * @param ip the IP address of the other node
     * @param port the port of the other node
     */
    public static QueryKey getQueryKey(SocketAddress address) {
        InetAddress ip = ((InetSocketAddress)address).getAddress();
        int port = ((InetSocketAddress)address).getPort();
        return getQueryKey(ip, port);
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
        return new QueryKey(keychain.getSecretKey().getKeyBytes(ip,port), true);
    }

    /** Generates a QueryKey for a given IP:Port combo.
     *  For a given IP:Port combo, using a different QueryKeyGenerator
     *  will result in a different QueryKey.  The return value is constructed
     *  with prepareForNet equal to true.
     *
     * @param ip the IP address of the other node
     * @param port the port of the other node
     */
    public static QueryKey getQueryKey(InetAddress ip, int port,
                                       QueryKeyGenerator keyGen) {
        return new QueryKey(keyGen.getKeyBytes(ip, port), true);
    }

    public static QueryKey getQueryKey(byte[] data) {
        return new QueryKey(keychain.getSecretKey().getKeyBytes(data), true);
    }
    
    // -------------------------------------------
    // -- FACTORY METHOD
    
    /** Returns a new QueryKeyGenerator with random secret key(s),
     *  using the default QueryKeyGenerator implementation.
     */
    public static QueryKeyGenerator createKeyGenerator() {
        return new TEAQueryKeyGenerator();
    }
    
    
    private static class TEAFactory implements QKGeneratorFactory {
        public QueryKeyGenerator createQueryKeyGenerator() {
            return createKeyGenerator();
        }
    }
    
    /**
     * A simple keychain that does not expire its keys.
     */
    private static class SimpleKeychain implements QKGeneratorKeychain {
        private final QueryKeyGenerator []keys = 
            new QueryKeyGenerator[]{new TEAQueryKeyGenerator()};
        public QueryKeyGenerator[] getValidQueryKeyGenerators() {
            return keys;
        }
        
        public QueryKeyGenerator getSecretKey() {
            return keys[0];
        }
    }
}
