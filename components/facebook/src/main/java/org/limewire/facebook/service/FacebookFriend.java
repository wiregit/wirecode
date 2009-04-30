package org.limewire.facebook.service;

import java.util.Map;
import java.util.Collection;
import java.net.URI;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.json.JSONObject;

public class FacebookFriend implements Friend, FriendPresence {
    private final JSONObject friend;

    public FacebookFriend(JSONObject friend) {
        this.friend = friend;
    }
    
    @Override
    public String getId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getRenderName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getFirstName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setName(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAnonymous() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Network getNetwork() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, FriendPresence> getFriendPresences() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Friend getFriend() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getPresenceId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Feature> getFeatures() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
