package org.limewire.core.api.search.browse;

import java.util.Collection;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;

public interface BrowseSearchFactory {
    
    /**
     * Browses all presences of a friend, and keeps status up-to-date as the
     * presences change. This must be a non-anonymous friend.
     */
    BrowseSearch createFriendBrowseSearch(Friend friend);
    
    /** Browses a single presence. Status-changes of the presence are not kept up-to-date. */
    BrowseSearch createBrowseSearch(FriendPresence presence);
    
    /** Browses many presence. Status-changes of the presences are not kept up-to-date. */  
    BrowseSearch createBrowseSearch(Collection<FriendPresence> presences);
    
    /** Browses all known friends.  Status changes of the friends are kept up-to-date. */
    BrowseSearch createAllFriendsBrowseSearch();
   
    
}
