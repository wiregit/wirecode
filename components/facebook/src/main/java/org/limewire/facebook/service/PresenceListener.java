package org.limewire.facebook.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

@Singleton
public class PresenceListener implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(PresenceListener.class);
    
    private final EventBroadcaster<FriendEvent> knownBroadcaster;
    private final EventBroadcaster<FriendEvent> availableBroadcaster;
    private final EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster;
    private final String postFormID;
    private final FacebookFriendConnection connection;
    private final BuddyListResponseDeserializerFactory buddyListResponseDeserializerFactory;
//    private final ConcurrentMap<String, FacebookFriend> knownFriends = new ConcurrentHashMap<String, FacebookFriend>();
//    private final ConcurrentMap<String, FacebookFriend> availFriends = new ConcurrentHashMap<String, FacebookFriend>();
    
    @AssistedInject
    PresenceListener(@Assisted String postFormID,
                     @Assisted FacebookFriendConnection connection,
                     @Named("known") EventBroadcaster<FriendEvent> knownBroadcaster,
                     @Named("available") EventBroadcaster<FriendEvent> availableBroadcaster,
                     EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster,                     
                     BuddyListResponseDeserializerFactory buddyListResponseDeserializerFactory) {
        this.knownBroadcaster = knownBroadcaster;
        this.availableBroadcaster = availableBroadcaster;
        this.friendPresenceBroadcaster = friendPresenceBroadcaster;
        this.postFormID = postFormID;
        this.connection = connection;
        this.buddyListResponseDeserializerFactory = buddyListResponseDeserializerFactory;
    }

    public void run() {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("buddy_list", "1"));
        nvps.add(new BasicNameValuePair("notifications", "1"));
        nvps.add(new BasicNameValuePair("force_render", "true"));
        // not sent in pidgin
//        nvps.add(new BasicNameValuePair("post_form_id", postFormID));
        nvps.add(new BasicNameValuePair("user", connection.getUID()));
        nvps.add(new BasicNameValuePair("popped_out", "true"));
        
        try{
            LOG.debugf("posting buddy list request: {0}", nvps);
            String responseStr = connection.httpPOST("http://www.facebook.com", "/ajax/presence/update.php", nvps);
            if (responseStr == null) {
                LOG.debug("no response for buddy list post");
                return;
            } 
            LOG.debugf("buddy list response: {0}", responseStr);
                
            BuddyListResponseDeserializer deserializer = buddyListResponseDeserializerFactory.create(connection);
            Map<String, FacebookFriend> onlineFriends = deserializer.deserialize(responseStr);
            for(Friend friend : connection.getUsers()) {
                if(!onlineFriends.containsKey(friend.getId())) {
                    removePresence((FacebookFriend)connection.getUser(friend.getId()));
                }
            }
            for(String friend : onlineFriends.keySet()) {
                if(connection.getUser(friend) == null) {
                    addPresence(onlineFriends.get(friend));
                }        
            }
            
            // TODO updatePresence
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
//        connectionListeners.addListener(new EventListener<XMPPConnectionEvent>() {
//            @Override
//            public void handleEvent(XMPPConnectionEvent event) {
//                switch(event.getType()) {
//                case DISCONNECTED:
//                    for(User user : event.getSource().getUsers()) {
//                        removeKnownFriend(user, false);
//                    }
//                    break;
//                }
//            }
//        });
    }
    
//    private void addKnownFriend(User user) {
//        if (knownFriends.putIfAbsent(user.getId(), user) == null) {
//            user.addPresenceListener(presenceListener);
//            knownBroadcaster.broadcast(new FriendEvent(user, FriendEvent.Type.ADDED));
//        }
//    }
    
//    private void removeKnownFriend(User user, boolean delete) {
//        if (knownFriends.remove(user.getId()) != null) {
//            if(delete) {
//                knownBroadcaster.broadcast(new FriendEvent(user, FriendEvent.Type.DELETE));
//            }
//            knownBroadcaster.broadcast(new FriendEvent(user, FriendEvent.Type.REMOVED));
//        }
//    }
    
    private void updatePresence(FriendPresence presence) {
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.UPDATE));
    }
    
    private void addPresence(FacebookFriend presence) {
        Friend friend = presence.getFriend();
        connection.userAvailable(friend.getId(), presence);
        availableBroadcaster.broadcast(new FriendEvent(presence.getFriend(), FriendEvent.Type.ADDED));
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.ADDED));
    }
    
    private void removePresence(FriendPresence presence) {
        Friend friend = presence.getFriend();
        connection.userUnavailable(friend.getId());
        availableBroadcaster.broadcast(new FriendEvent(presence.getFriend(), FriendEvent.Type.REMOVED));
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.REMOVED));
    }

//    Collection<Friend> getKnownFriends() {
//        return Collections.unmodifiableCollection(knownFriends.values());        
//    }
//
//    Collection<Friend> getAvailableFriends() {
//        return Collections.unmodifiableCollection(availFriends.values());
//    }
//    
//    Set<String> getAvailableFriendIds() {
//        return Collections.unmodifiableSet(availFriends.keySet());
//    }
}
