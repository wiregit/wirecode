package org.limewire.security;

import java.io.IOException;
import java.io.OutputStream;
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
    public boolean isFor(TokenData data);
    
    /**
     * The TokenProvider is a factory interface to create SecurityTokens
     */
    public static interface TokenProvider {
        
        /**
         * Creates and returns a SecurityToken for the given SocketAddress
         */
        public SecurityToken getSecurityToken(TokenData data);
        
        public TokenData getTokenData(SocketAddress addr);
        
    }
    
    public static interface TokenData {
        public byte [] getData();
    }
    
    /**
     * A TokenProvider implmenetation that creates QueryKey based SecurityToken
     */
    public static class QueryKeyProvider implements TokenProvider {

        public SecurityToken getSecurityToken(TokenData data) {
            return QueryKey.getQueryKey(data, true);
        }
        
        public TokenData getTokenData(SocketAddress addr) {
            return new QueryKey.GUESSTokenData(addr);
        }
    }
}
