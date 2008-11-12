package org.limewire.xmpp.api.client;

import org.limewire.core.api.friend.Friend;
import org.limewire.listener.EventListener;

/**
 * Represents a user ("friend") in a persons roster
 */
public interface User extends Friend {
    
    public enum EventType {USER_ADDED, USER_UPDATED, USER_REMOVED}

    /**
     * Allows the xmpp service user to register a listener for presence changes of this user
     * @param presenceListener
     */
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener);

}
