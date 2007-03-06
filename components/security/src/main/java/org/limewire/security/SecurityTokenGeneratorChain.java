package org.limewire.security;

/**
 * A token generator chain that holds the valid security token generators.
 */
interface SecurityTokenGeneratorChain {
    /**
     * @return the <tt>SecurityTokenGenerator</tt>'s that are currently valid.
     */
    public SecurityTokenGenerator [] getValidSecurityTokenGenerators();
    
    /**
     * @return the current token generator
     */
    public SecurityTokenGenerator getCurrentTokenGenerator();
}