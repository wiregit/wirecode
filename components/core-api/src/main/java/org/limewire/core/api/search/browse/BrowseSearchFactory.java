package org.limewire.core.api.search.browse;

import java.util.Collection;

import org.limewire.friend.api.FriendPresence;

public interface BrowseSearchFactory {
    
    /** Browses a single presence. Status-changes of non-anonymous friends are kept up-to-date.
     *  Status-changes of anonymous presences are not kept up-to-date. */
    BrowseSearch createBrowseSearch(FriendPresence presence);
    
    /** Browses many presences. Only status-changes of non-anonymous friends in the collection are kept 
     *  up-to-date. Status-changes of anonymous presences are not kept up-to-date.
     */  
    BrowseSearch createBrowseSearch(Collection<FriendPresence> presences);
}
