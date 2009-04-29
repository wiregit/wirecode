package org.limewire.core.impl;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.FriendPresence;

import com.limegroup.gnutella.RemoteFileDesc;

/**
 * A RemoteFileDesc implementation for RemoteHosts. 
 */
public class RemoteHostRFD implements RemoteHost {
    
    private RemoteFileDesc remoteFileDesc;

    private FriendPresence friendPresence;
    
    public RemoteHostRFD(RemoteFileDesc remoteFileDesc, FriendPresence friendPresence) {
        this.remoteFileDesc = remoteFileDesc;
        this.friendPresence = friendPresence;
    }
    
    @Override
    public FriendPresence getFriendPresence() {
        return friendPresence;
    }

    @Override
    public boolean isBrowseHostEnabled() {
        return remoteFileDesc.isBrowseHostEnabled();
    }

    @Override
    public boolean isChatEnabled() {
        return !friendPresence.getFriend().isAnonymous();
    }

    @Override
    public boolean isSharingEnabled() {
        return !friendPresence.getFriend().isAnonymous();
    }

}
