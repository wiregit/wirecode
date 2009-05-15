package org.limewire.ui.swing.friends.chat;

import java.net.URI;
import java.util.Collection;

import org.limewire.core.api.friend.AbstractFriendPresence;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureTransport;

public class MockPresence extends AbstractFriendPresence implements FriendPresence {
    private String status;
    private final Friend user;
    private Mode mode;
    private String jid;
    private int priority;
    
    MockPresence(Friend user, Mode mode, String status, String jid) {
        this.user = user;
        this.mode = mode;
        this.status = status;
        this.jid = jid;
        this.priority = 0;
    }

    public Friend getUser() {
        return user;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        return priority;
    }

    // package private for unit tests
    void setPriority(int priority) {
        this.priority = priority;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Type getType() {
        return Type.available;
    }

    @Override
    public Friend getFriend() {
        return user;
    }

    @Override
    public String getPresenceId() {
        return jid;
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

    @Override
    public <T extends Feature<U>, U> FeatureTransport<U> getTransport(Class<T> feature) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
