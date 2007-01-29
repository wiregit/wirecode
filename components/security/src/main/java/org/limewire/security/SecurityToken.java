package org.limewire.security;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;

/**
 * A SecurityToken authenticates a host based on its IP:Port or a other
 * pieces of data.
 * 
 * TODO make api similar to MessageDigest so the secret byte can be updated
 * by new input
 */
public interface SecurityToken {
    
    /**
     * Returns the length of the SecurityToken in byte
     */
    public int getTokenLength();
    
    /**
     * Returns the SecurityToken as byte array
     */
    public byte[] getBytes();
    
    /**
     * Writes the SecurityToken to the output stream
     */
    public void write(OutputStream out) throws IOException;
    
    /** 
     * Validates that a SecurityToken was generated for the given SocketAddress
     */
    public boolean isFor(SocketAddress addr);
    
    /** 
     * Validates that a SecurityToken was generated for the given IP:Port
     */
    public boolean isFor(InetAddress addr, int port);
    
    /**
     * The TokenProvider is a factory interface to create SecurityTokens
     */
    public static interface TokenProvider {
        
        /**
         * Creates and returns a SecurityToken for the given SocketAddress
         */
        public SecurityToken getSecurityToken(SocketAddress addr);
        
        /**
         * Creates and returns a SecurityToken for the given IP:Port
         */
        public SecurityToken getSecurityToken(InetAddress addr, int port);
        
        /**
         * Returns a SecurityToken for the given data.
         */
        public SecurityToken getSecurityToken(byte[] data);
        
    }
    
    /**
     * A TokenProvider implmenetation that creates QueryKey based SecurityToken
     */
    public static class QueryKeyProvider implements TokenProvider {

        public SecurityToken getSecurityToken(SocketAddress addr) {
            return QueryKey.getQueryKey(addr);
        }
        
        public SecurityToken getSecurityToken(InetAddress addr, int port) {
            return QueryKey.getQueryKey(addr, port);
        }

        public SecurityToken getSecurityToken(byte[] data) {
            return QueryKey.getQueryKey(data);
        }
    }
}
