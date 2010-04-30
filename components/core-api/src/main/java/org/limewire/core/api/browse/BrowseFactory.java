package org.limewire.core.api.browse;

import org.limewire.friend.api.FriendPresence;
import org.limewire.io.Address;
import org.limewire.io.Connectable;

/**
 * Factory for creating Gnutella browses.
 */
public interface BrowseFactory {
    /**
     * Creates a Browse from the given FriendPresence.
     */
    Browse createBrowse(FriendPresence friendPresence);
    
    /**
     * Creates a Browse from the given Connectable.
     */
    Browse createBrowse(Connectable connectable);
    
    /**
     * Creates a Browse from the given Address and GUID.
     */
    Browse createBrowse(Address address, byte[] guid);
}
