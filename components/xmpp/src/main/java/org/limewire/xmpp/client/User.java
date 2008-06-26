package org.limewire.xmpp.client;

/**
 * Represents a user ("buddy") in a persons roster
 */
public interface User {

    /**
     * @return the id of the user.  user-ids have the form <code>user@host.com</code>
     */
    String getId();

    /**
     * @return the friendly user given name to the user; can be null.
     */
    String getName();

    /**
     * Allows the xmpp service user to register a listener for presence changes of this user
     * @param presenceListener
     */
    void addPresenceListener(PresenceListener presenceListener);

}
