package org.limewire.facebook.service;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.feature.Feature;

public class FacebookFriend implements Friend, FriendPresence {
    private final String id;
    private final JSONObject friend;

    public FacebookFriend(String id, JSONObject friend) {
        this.id = id;
        this.friend = friend;
    }
    
    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        try {
            return friend.getString("name");
        } catch (JSONException e) {
            return "";
        }
    }

    @Override
    public String getRenderName() {
        return getName();
    }

    @Override
    public String getFirstName() {
        try {
            return friend.getString("firstName");
        } catch (JSONException e) {
            return "";
        }
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, FriendPresence> getFriendPresences() {
        return Collections.singletonMap(getId(), (FriendPresence)this);
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
    public Collection<Feature> getFeatures() {
        return Collections.emptyList();
    }

    @Override
    public Feature getFeature(URI id) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasFeatures(URI... id) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addFeature(Feature feature) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeFeature(URI id) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
