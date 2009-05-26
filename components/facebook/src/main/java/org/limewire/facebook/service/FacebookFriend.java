package org.limewire.facebook.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.impl.AbstractFriend;
import org.limewire.util.StringUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FacebookFriend extends AbstractFriend {
    
    private final String id;
    private final JSONObject friend;
    private final Network network;
    private final Map<String, FacebookFriendPresence> presenceMap = new ConcurrentHashMap<String, FacebookFriendPresence>();
    
    private final boolean hasLimeWireAppInstalled;
    private final FeatureRegistry featureRegistry;

    
    @AssistedInject
    public FacebookFriend(@Assisted String id, @Assisted JSONObject friend,
                          @Assisted Network network, @Assisted boolean hasLimeWireAppInstalled,
                          FeatureRegistry featureRegistry) {
        this.id = id;
        this.friend = friend;
        this.network = network;
        this.hasLimeWireAppInstalled = hasLimeWireAppInstalled;
        this.featureRegistry = featureRegistry;
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
        return null;
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        return Collections.unmodifiableMap(new HashMap<String, FriendPresence>(presenceMap));
    }

    @Override
    public Network getNetwork() {
        return network;
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
        return !presenceMap.isEmpty();
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
    
    void addPresence(FacebookFriendPresence presence) {
        presenceMap.put(presence.getPresenceId(), presence);    
    }
    
    void removePresence(FacebookFriendPresence presence) {
        Collection<Feature> features = presence.getFeatures();
        for(Feature feature : features) {
            featureRegistry.get(feature.getID()).removeFeature(presence);
        }
        presenceMap.remove(presence.getPresenceId());
        
    }

    public boolean hasLimeWireAppInstalled() {
        return hasLimeWireAppInstalled;
    }
}
