package org.limewire.core.impl.friend;

import org.limewire.friend.api.Friend;

class BittorrentFriend implements Friend {

    private final String id;

    public BittorrentFriend(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return getId();
    }

    @Override
    public String getRenderName() {
        return getName();
    }
}
