package org.limewire.facebook.service;

import java.util.Collections;
import java.util.Map;

import org.json.JSONObject;
import org.limewire.core.api.friend.AbstractFriendPresence;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.PresenceEvent;

public class FacebookFriend extends AbstractFriendPresence implements Friend, FriendPresence {
    private final String id;
    private final JSONObject friend;
    private final Network network;

    public FacebookFriend(String id, JSONObject friend, Network network,
                          EventBroadcaster<FeatureEvent> featureSupport) {
        super(featureSupport);
        this.id = id;
        this.friend = friend;
        this.network = network;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return friend.optString("name", "");
    }

    @Override
    public String getRenderName() {
        return getName();
    }

    @Override
    public String getFirstName() {
        return friend.optString("first_name", "");
    }

    @Override
    public void setName(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public Map<String, FriendPresence> getFriendPresences() {
        return Collections.singletonMap(getId(), (FriendPresence)this);
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
        return this;
    }

    @Override
    public boolean hasActivePresence() {
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return true;
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        return Collections.singletonMap(getPresenceId(), (FriendPresence)this);
    }

    @Override
    public boolean isSubscribed() {
        return true;
    }

    @Override
    public Friend getFriend() {
        return this;
    }

    @Override
    public String getPresenceId() {
        return getId();
    }

    @Override
    public Type getType() {
        return Type.available;
    }

    @Override
    public String getStatus() {
        return friend.optString("status", null);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Mode getMode() {
        return Mode.available;  // TODO figure out how fb does mode
    }
}
