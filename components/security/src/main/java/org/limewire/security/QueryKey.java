package org.limewire.security;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.limewire.security.SecurityToken.TokenData;
import org.limewire.util.ByteOrder;

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
public final class QueryKey extends AbstractQueryKey<TokenData> {

    /** As detailed by the GUESS spec.
     */
    public static final int MIN_QK_SIZE_IN_BYTES = 4;
    /** As detailed by the GUESS spec.
     */
    public static final int MAX_QK_SIZE_IN_BYTES = 16;
    
    /**
     * Cached value to make hashCode() much faster.
     */
    private final int _hashCode;

    public QueryKey(byte[] key) throws InvalidSecurityTokenException {
        super(key.clone());
        _hashCode = genHashCode(getBytes());
    }
    
    private int genHashCode(byte [] key) {
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
        return code;
    }
    
    protected byte [] getFromMAC(byte [] key, TokenData ignored) {
        for (int i = key.length - 1; i >= 0; --i) {
            // The old prepareForNetwork() seemed to leave cobbs encoding to get
            // of nulls?  TODO: is it okay to leave nulls alone?
            if (key[i] == 0x1c) {
                key[i] = (byte) (0xFA);
            }
        }
        return key;
    }
    
    
    public boolean isFor(SocketAddress address) {
        InetAddress ip = ((InetSocketAddress)address).getAddress();
        int port = ((InetSocketAddress)address).getPort();
        return isFor(ip, port);
    }
    
    public boolean isFor(InetAddress ip, int port) {
        return isFor(new GUESSTokenData(ip, port));
    }
    
    public int hashCode() {
       return _hashCode;
    }

    /** Returns a String with the QueryKey represented in hexadecimal.
     */
    public String toString() {
        return "{Query Key: " + (new BigInteger(1, getBytes())).toString(16) + "}";
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
    
    protected boolean isValidBytes(byte [] key) {
        return isValidQueryKeyBytes(key);
    }


    /** Generates a QueryKey for a given SocketAddress.
     *  For a given SocketAddress, using a different SecretKey and/or SecretPad
     *  will result in a different QueryKey.  The return value is constructed
     *  with prepareForNet equal to true.
     *  
     * @param ip the IP address of the other node
     * @param port the port of the other node
     */
    public QueryKey (SocketAddress address) {
        this(((InetSocketAddress)address).getAddress(),
                ((InetSocketAddress)address).getPort());
    }
    
    /** Generates a QueryKey for a given IP:Port combo.
     *  For a given IP:Port combo, using a different SecretKey and/or SecretPad
     *  will result in a different QueryKey.  The return value is constructed
     *  with prepareForNet equal to true.
     *  
     * @param ip the IP address of the other node
     * @param port the port of the other node
     */
    public QueryKey (InetAddress ip, int port) {
        this(new GUESSTokenData(ip,port));
    }
    
    public QueryKey(TokenData data) {
        super(data);
        _hashCode = genHashCode(getBytes());
    }
    
    /** Returns a new QueryKeyGenerator with random secret key(s),
     *  using the default QueryKeyGenerator implementation.
     */
    public static QueryKeyGenerator createKeyGenerator() {
        return new TEAQueryKeyGenerator();
    }
    
    /**
     * Token data necessary for the creation of Query keys for the
     * GUESS protocol.
     */
    public static class GUESSTokenData implements SecurityToken.TokenData {
        private final byte[] data;
        
        public GUESSTokenData(SocketAddress address) {
            this(((InetSocketAddress)address).getAddress(),
            ((InetSocketAddress)address).getPort());
        }
        
        public GUESSTokenData(InetAddress addr, int port) {
            // get all the input bytes....
            byte[] ipBytes = addr.getAddress();
            int ipInt = 0;
            // Load the first 4 bytes into ipInt in little-endian order,
            // with the twist that any negative bytes end up flipping
            // all of the higher order bits, but we don't care.
            for(int i=3; i >= 0; --i) {
                ipInt ^= ipBytes[i] << (i << 3);
            }
            
            // Start out with 64 bits |0x00|0x00|port(2bytes)|ip(4bytes)|
            // and encrypt it with our secret key material.
            data = new byte[8];
            ByteOrder.int2beb(port, data, 0);
            ByteOrder.int2beb(ipInt, data, 4);
            
        }
        
        public byte [] getData() {
            return data;
        }
    }
}
