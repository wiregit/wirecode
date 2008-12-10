package org.limewire.core.api.endpoint;

import org.limewire.core.api.friend.FriendPresence;

public interface RemoteHost {
    boolean isAnonymous();
    
    boolean isBrowseHostEnabled();
    
    boolean isChatEnabled();
    
    boolean isSharingEnabled();
    
    public FriendPresence getFriendPresence();
}
