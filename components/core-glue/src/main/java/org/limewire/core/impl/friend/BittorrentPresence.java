package org.limewire.core.impl.friend;

import java.net.URI;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;

import com.limegroup.gnutella.Uploader;

/**
 * Allows the bittorrent uploader to be converted into a FriendPresence that
 * does no support any standard friend/gnutella features.
 */
public class BittorrentPresence implements FriendPresence {

    private final String id;
    private final Friend friend;

    public BittorrentPresence(Uploader uploader) {
        this.id = uploader.getUrn().toString();
        this.friend = new BittorrentFriend(id);
    }

    @Override
    public Feature getFeature(URI id) {
        return null;
    }

    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public String getPresenceId() {
        return id;
    }

    @Override
    public boolean hasFeatures(URI... id) {
        return false;
    }
}
