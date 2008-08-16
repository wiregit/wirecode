package org.limewire.xmpp.api.client;

/**
 * Represents a user ("buddy") in a persons roster
 */
public interface User {
    
    public enum EventType {USER_ADDED, USER_UPDATED, USER_REMOVED}

    /**
     * @return the id of the user.  user-ids have the form <code>user@host.com</code>
     */
    public String getId();

    /**
     * @return the friendly user given name to the user; can be null.
     */
    public String getName();

    /**
     * Allows the xmpp service user to register a listener for presence changes of this user
     * @param presenceListener
     */
    public void addPresenceListener(PresenceListener presenceListener);

}
