package org.limewire.core.api.friend;

import org.limewire.io.Address;

/**
 * A presence for a friend.
 * One friend can have multiple presences.
 */
public interface FriendPresence {
    
    /**
     * Returns the containing friend.
     */
    Friend getFriend();
    
    /**
     * The ID of this specific presence.
     */
    String getPresenceId();
    
    /**
     * An address at which this presence can be contacted.
     */
    Address getPresenceAddress();

}
