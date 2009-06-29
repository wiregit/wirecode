package org.limewire.core.impl.friend;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.listener.EventListener;
import org.limewire.util.Objects;

public class GnutellaFriend implements Friend {

    private final String name;
    private final String renderName;
    private final String id;
    private final Map<String, FriendPresence> map; 

    public GnutellaFriend(String name, String renderName, String id,
            FriendPresence presence) {
        this.name = Objects.nonNull(name, "name");
        this.renderName = Objects.nonNull(renderName, "renderName");
        this.id = id;
        Map<String, FriendPresence> map = new HashMap<String, FriendPresence>(1);
        map.put(presence.getPresenceId(), presence);
        this.map = Collections.unmodifiableMap(map);
    }

    @Override
    public boolean isAnonymous() {
        return true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getRenderName() {
        return renderName;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public Network getNetwork() {
        return null;
    }

    @Override
    public String getFirstName() {
        return getName();
    }

    @Override
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return null;
    }

    @Override
    public void setChatListenerIfNecessary(IncomingChatListener listener) {
    }

    @Override
    public void removeChatListener() {
    }

    @Override
    public FriendPresence getActivePresence() {
        return null;
    }

    @Override
    public boolean hasActivePresence() {
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return false;
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        return map;
    }

    @Override
    public boolean isSubscribed() {
        return false;
    }
    
    @Override
    public String toString() {
        return "renderName[" + renderName + "], name[" + name + "], id[" + id + "]"; 
    }
}
