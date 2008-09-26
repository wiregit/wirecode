package org.limewire.xmpp.api.client;

import java.util.Map;

import org.limewire.core.api.friend.Friend;

/**
 * Represents a user ("buddy") in a persons roster
 */
public interface User extends Friend {
    
    public enum EventType {USER_ADDED, USER_UPDATED, USER_REMOVED}

    /**
     * Allows the xmpp service user to register a listener for presence changes of this user
     * @param presenceListener
     */
    public void addPresenceListener(PresenceListener presenceListener);
    
    /**
     * Provides an indication of whether the supplied String (assumed to be a Presence jid)
     * belongs to this user
     * @param jid
     * @return true if the the jid belongs to this user, otherwise false
     */
    public boolean jidBelongsTo(String jid);

    /**
     * @return an unmodifiable map of all <code>Presence</code>s for this <code>User</code>. 
     * Keys are fully qualified jids of the form <code>"user@domain.com/resourceXYZ"</code>
     */
    public Map<String, Presence> getPresences();
}
