package org.limewire.xmpp.api.client;

import java.util.Map;

import org.limewire.core.api.friend.Friend;

/**
 * Represents a user ("friend") in a persons roster
 */
public interface User extends Friend {
    
    public enum EventType {USER_ADDED, USER_UPDATED, USER_REMOVED}

    /**
     * Allows the xmpp service user to register a listener for presence changes of this user
     * @param presenceListener
     */
    public void addPresenceListener(PresenceListener presenceListener);
    
    /**
     * @return an unmodifiable map of all <code>Presence</code>s for this <code>User</code>. 
     * Keys are fully qualified jids of the form <code>"user@domain.com/resourceXYZ"</code>
     */
    public Map<String, Presence> getPresences();
}
