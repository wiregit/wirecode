package com.limegroup.gnutella.settings;

/**
 * Listeners for possible updates to the settings
 * that can be modified through simpp. 
 */
public interface SimppSettingsListener {
    
    /**
     * Notification that the settings may have changed.
     */
    public void settingsUpdated();
}
