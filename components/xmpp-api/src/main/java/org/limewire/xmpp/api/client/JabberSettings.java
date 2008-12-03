package org.limewire.xmpp.api.client;

/**
 * This class provides a means for core/component code to access XMPPSettings.
 */
public interface JabberSettings {
    /**
     * Returns true if the user has decided to set the do not disturb option. 
     */
    public boolean isDoNotDisturbSet();
}
