package org.limewire.facebook.service;

import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.impl.AbstractFriendPresence;
import org.limewire.listener.EventBroadcaster;
import org.limewire.util.StringUtils;

public class FacebookFriendPresence extends AbstractFriendPresence {

    private final FacebookFriend friend;

    public FacebookFriendPresence(FacebookFriend friend, EventBroadcaster<FeatureEvent> featureEventBroadcaster) {
        super(featureEventBroadcaster);
        this.friend = friend;
    }
    
    @Override
    public FacebookFriend getFriend() {
        return friend;
    }

    @Override
    public Mode getMode() {
        return Mode.available;
    }

    @Override
    public String getPresenceId() {
        return friend.getId();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getStatus() {
        return null;
    }

    @Override
    public Type getType() {
        return Type.available;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
