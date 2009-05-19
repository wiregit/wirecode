package org.limewire.friend.impl;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.MutableFriendManager;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.client.impl.PresenceImpl;
import org.limewire.xmpp.client.impl.XMPPFriendImpl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FriendListListeners {
    
    private final PresenceListener presenceListener = new PresenceListener();
    private final MutableFriendManager friendManager;
    private final EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster;
    
    @Inject
    FriendListListeners(MutableFriendManager friendManager, 
            EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster) {
        this.friendManager = friendManager;
        this.friendPresenceBroadcaster = friendPresenceBroadcaster;
    }
    
    @Inject void register(ListenerSupport<RosterEvent> rosterListeners,
                          ListenerSupport<FriendConnectionEvent> connectionListeners) {
        rosterListeners.addListener(new EventListener<RosterEvent>() {
            @Override
            public void handleEvent(RosterEvent event) {
                XMPPFriendImpl friend = event.getData();

                switch(event.getType()) {
                case USER_ADDED:
                    if (friend.isSubscribed()) {
                        addKnownFriend(friend);
                    }
                    break;
                case USER_UPDATED:
                    if (friend.isSubscribed()) {
                        addKnownFriend(friend);
                    } else {
                        friendManager.removeKnownFriend(friend, true);
                    }
                    break;
                case USER_DELETED:
                    friendManager.removeKnownFriend(friend, true);
                    break;
                }
            }
        });
        
        connectionListeners.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                switch(event.getType()) {
                case DISCONNECTED:
                    for(Friend user : event.getSource().getFriends()) {
                        friendManager.removeKnownFriend(user, false);
                    }
                    break;
                }
            }
        });
    }

    
    public void addKnownFriend(XMPPFriendImpl friend) {
        friend.addPresenceListener(presenceListener);
        friendManager.addKnownFriend(friend);
    }
    
    private void updatePresence(FriendPresence presence) {
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.UPDATE));
    }
    
    private void addPresence(FriendPresence presence) {
        Friend friend = presence.getFriend();
        if(friend.getPresences().size() == 1) {
            friendManager.addAvailableFriend(friend);
        }
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.ADDED));
    }
    
    private void removePresence(PresenceImpl presence) {
        XMPPFriendImpl friend = presence.getFriend();
        if(!friend.isSignedIn()) {
            friendManager.removeAvailableFriend(friend);
        }
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.REMOVED));
    }

    
    private class PresenceListener implements EventListener<PresenceEvent> {
        @Override
        public void handleEvent(PresenceEvent event) {
            switch (event.getData().getType()) {
                case available:
                    switch (event.getType()) {
                        case PRESENCE_NEW:
                            addPresence(event.getData());
                            break;
                        case PRESENCE_UPDATE:
                            updatePresence(event.getData());
                            break;
                    }
                    break;
                case unavailable:
                    removePresence((PresenceImpl)event.getData());
                    break;
            }
        }
    }
}
