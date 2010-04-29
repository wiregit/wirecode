package org.limewire.mojito.message2;

import org.limewire.security.SecurityToken;

/**
 * A mix-in interface for {@link Message}s that provide a {@link SecurityToken}.
 */
public interface SecurityTokenProvider {
    
    /**
     * Returns the {@link SecurityToken}.
     */
    public SecurityToken getSecurityToken();
}
