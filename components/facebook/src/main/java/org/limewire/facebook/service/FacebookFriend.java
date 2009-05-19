package org.limewire.facebook.service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONObject;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.core.api.friend.impl.AbstractFriend;
import org.limewire.util.StringUtils;

public class FacebookFriend extends AbstractFriend {
    
    private final String id;
    private final JSONObject friend;
    private final Network network;
    
    /**
     * The presence of the facebook friend, null when friend is not online.
     */
    private final AtomicReference<FacebookFriendPresence> presence = new AtomicReference<FacebookFriendPresence>();
    
    private final boolean hasLimeWireAppInstalled;

    public FacebookFriend(String id, JSONObject friend, Network network, boolean hasLimeWireAppInstalled) {
        this.id = id;
        this.friend = friend;
        this.network = network;
        this.hasLimeWireAppInstalled = hasLimeWireAppInstalled;
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
    public MessageWriter createChat(MessageReader reader) {
        return null;
    }

    @Override
    public FriendPresence getActivePresence() {
        return presence.get();
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        FriendPresence copy = presence.get();
        if (copy != null) {
            return Collections.singletonMap(id, copy);
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public boolean hasActivePresence() {
        return presence.get() != null;
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return presence.get() != null;
    }

    @Override
    public boolean isSubscribed() {
        return true;
    }

    @Override
    public void removeChatListener() {
    }

    @Override
    public void setChatListenerIfNecessary(IncomingChatListener listener) {
    }

    @Override
    public void setName(String name) {
    }
    
    void setPresence(FacebookFriendPresence facebookFriendPresence) {
        presence.set(facebookFriendPresence);
        if (hasLimeWireAppInstalled) {
            facebookFriendPresence.addFeature(new LimewireFeature());
        }
    }
    
    /**
     * @return null if friend is not available for chat
     */
    public FacebookFriendPresence getFacebookPresence() {
        return presence.get();
    }
}
