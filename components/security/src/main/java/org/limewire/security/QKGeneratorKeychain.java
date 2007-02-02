package org.limewire.security;

/**
 * A keychain that holds the valid query key generators.
 */
interface QKGeneratorKeychain {
    /**
     * @return the <tt>QueryKeyGenerator</tt>'s that are currently valid.
     */
    public QueryKeyGenerator [] getValidQueryKeyGenerators();
    
    /**
     * @return the current secret key.
     */
    public QueryKeyGenerator getSecretKey();
}