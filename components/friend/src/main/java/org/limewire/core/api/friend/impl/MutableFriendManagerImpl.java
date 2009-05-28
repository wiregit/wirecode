package org.limewire.core.api.friend.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.MutableFriendManager;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class MutableFriendManagerImpl implements MutableFriendManager {

    private static final Log LOG = LogFactory.getLog(MutableFriendManagerImpl.class);
    
    private final EventBroadcaster<FriendEvent> knownBroadcaster;
    private final EventBroadcaster<FriendEvent> availableBroadcaster;
    private final ConcurrentMap<String, Friend> knownFriends = new ConcurrentHashMap<String, Friend>();
    private final ConcurrentMap<String, Friend> availFriends = new ConcurrentHashMap<String, Friend>();
    
    @Inject
    public MutableFriendManagerImpl(@Named("known") EventBroadcaster<FriendEvent> knownBroadcaster,
            @Named("available") EventBroadcaster<FriendEvent> availableBroadcaster,
            EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster) {
        this.knownBroadcaster = knownBroadcaster;
        this.availableBroadcaster = availableBroadcaster;
    }
    
    @Inject
    void register(ListenerSupport<FriendConnectionEvent> listenerSupport) {
        listenerSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                switch(event.getType()) {
                case DISCONNECTED:
                    for (Friend user :  availFriends.values()) {
                        removeAvailableFriend(user);
                    }
                    for (Friend user :  knownFriends.values()) {
                        removeKnownFriend(user, false);
                    }
                    break;
                }
            }
        });
    }
    
    @Override
    public void addKnownFriend(Friend friend) {
        if (knownFriends.putIfAbsent(friend.getId(), friend) == null) {
            LOG.debugf("adding known friend: {0}", friend);
            knownBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.ADDED));
        } else {
            LOG.debugf("not adding known friend: {0}", friend);
        }
    }
    
    @Override
    public void removeKnownFriend(Friend friend, boolean delete) {
        if (knownFriends.remove(friend.getId()) != null) {
            if(delete) {
                knownBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.DELETE));
            }
            knownBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.REMOVED));
        }
    }
    
    @Override
    public boolean containsAvailableFriend(String id) {
        return availFriends.containsKey(id);
    }

    @Override
    public FriendPresence getMostRelevantFriendPresence(String id) {
        Friend friend = availFriends.get(id);
        if(friend == null) {
            return null;
        } else {
            Collection<FriendPresence> presences = friend.getPresences().values();
            //TODO: this is not guarenteed to return the correct FriendPresence
            // if the user is logged in through two LWs with the same ID
            // Not really able to fix this without modifying the Browse/File Request
            for(FriendPresence nextPresence : presences) {
                if(nextPresence.hasFeatures(LimewireFeature.ID)) {
                    return nextPresence;
                }
            }
            return null;
        }
    }
    
    Collection<Friend> getKnownFriends() {
        return Collections.unmodifiableCollection(knownFriends.values());        
    }

    Collection<Friend> getAvailableFriends() {
        return Collections.unmodifiableCollection(availFriends.values());
    }
    
    Set<String> getAvailableFriendIds() {
        return Collections.unmodifiableSet(availFriends.keySet());
    }

    @Override
    public void addAvailableFriend(Friend friend) {
        if (availFriends.putIfAbsent(friend.getId(), friend) == null) {
            LOG.debugf("adding avail friend: {0}", friend);
            availableBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.ADDED));
        } else {
            LOG.debugf("not adding avail friend: {0}", friend);
        }
    }

    @Override
    public void removeAvailableFriend(Friend friend) {
        if (availFriends.remove(friend.getId()) != null) {
            availableBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.REMOVED));
        }
    }

}
