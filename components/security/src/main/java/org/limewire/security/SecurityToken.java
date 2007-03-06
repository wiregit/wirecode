package org.limewire.security;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;

/**
 * A SecurityToken authenticates a host based on its IP:Port or a other
 * pieces of data.
 * 
 */
public interface SecurityToken<T extends SecurityToken.TokenData> {
    
    /**
     * Returns the SecurityToken as byte array
     */
    public byte[] getBytes();
    
    /**
     * Writes the SecurityToken to the output stream
     */
    public void write(OutputStream out) throws IOException;
    
    /** 
     * Validates that a SecurityToken was generated for the given TokenData
     */
    public boolean isFor(T data);
    
    /**
     * The TokenProvider is a factory interface to create SecurityTokens
     */
    public static interface TokenProvider {
        
        /**
         * Creates and returns a SecurityToken for the given SocketAddress
         */
        public SecurityToken getSecurityToken(SocketAddress addr);
    }
    
    public static interface TokenData {
        public byte [] getData();
    }
    
    /**
     * A TokenProvider implementation that creates aQueryKey based SecurityTokens
     */
    public static class QueryKeyProvider implements TokenProvider {

        public SecurityToken getSecurityToken(SocketAddress addr) {
            return new QueryKey(addr);
        }
    }
}
