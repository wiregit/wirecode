package org.limewire.security;

/**
 * The QueryKey.SettingsProvider provides Settings 
 * for the QueryKey class
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