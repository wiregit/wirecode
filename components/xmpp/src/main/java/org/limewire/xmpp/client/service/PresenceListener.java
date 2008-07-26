package org.limewire.xmpp.client.service;

/**
 * Allows users of the xmpp service to listen for presence changes of
 * people in their roster.
 */
public interface PresenceListener {
    public void presenceChanged(Presence presence);
}
