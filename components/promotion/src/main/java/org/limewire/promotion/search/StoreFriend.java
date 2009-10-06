package org.limewire.promotion.search;

import java.util.Collections;
import java.util.Map;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.listener.EventListener;

/**
 * An implementation of Friend for the Lime Store.
 */
class StoreFriend implements Friend {

    private final FriendPresence presence;
    
    /**
     * Constructs a StoreFriend with the specified presence.
     */
    public StoreFriend(FriendPresence presence) {
        this.presence = presence;
    }
    
    @Override
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
        // Do nothing.
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return null;
    }

    @Override
    public FriendPresence getActivePresence() {
        return null;
    }

    @Override
    public String getFirstName() {
        return presence.getPresenceId();
    }

    @Override
    public String getId() {
        return presence.getPresenceId();
    }

    @Override
    public String getName() {
        return presence.getPresenceId();
    }

    @Override
    public Network getNetwork() {
        return null;
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        return Collections.singletonMap(presence.getPresenceId(), presence);
    }

    @Override
    public String getRenderName() {
        return presence.getPresenceId();
    }

    @Override
    public boolean hasActivePresence() {
        return false;
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return false;
    }

    @Override
    public boolean isSubscribed() {
        return false;
    }

    @Override
    public void removeChatListener() {
        // Do nothing.
    }

    @Override
    public void setChatListenerIfNecessary(IncomingChatListener listener) {
        // Do nothing.
    }

    @Override
    public void setName(String name) {
        // Do nothing.
    }
}
