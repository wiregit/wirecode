package org.limewire.core.api.search.browse;

import java.util.Collection;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;

public interface BrowseSearchFactory {
    
    /**
     * @param friend The friend to browse.  Can not be anonymous or null.
     */
    BrowseSearch createFriendBrowseSearch(Friend friend);
    
    /**
     * 
     * @param person The host to browse.  Can be a friend or anonymous.  Can not be null.
     */
    BrowseSearch createBrowseSearch(RemoteHost person);
    
    BrowseSearch createBrowseSearch(Collection<RemoteHost> people);
    
    BrowseSearch createAllFriendsBrowseSearch();
   
    
}
