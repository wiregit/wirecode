package org.limewire.security;

/**
 * The AddressSecurityToken.SettingsProvider provides Settings 
 * for the AddressSecurityToken class
 */
public interface SettingsProvider {
    
    /**
     * Time in milliseconds
     */
    public long getChangePeriod();
    
    /**
     * Time in milliseconds
     */
    public long getGracePeriod();
}