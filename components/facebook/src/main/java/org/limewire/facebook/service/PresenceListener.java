package org.limewire.facebook.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.MutableFriendManager;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

@Singleton
public class PresenceListener implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(PresenceListener.class);
    
    private final EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster;
    private final FacebookFriendConnection connection;
    private final BuddyListResponseDeserializer deserializer;

    private final MutableFriendManager friendManager;
    
    @AssistedInject
    PresenceListener(@Assisted FacebookFriendConnection connection,
                     MutableFriendManager friendManager,
                     EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster,                     
                     BuddyListResponseDeserializerFactory buddyListResponseDeserializerFactory) {
        this.friendManager = friendManager;
        this.friendPresenceBroadcaster = friendPresenceBroadcaster;
        this.connection = connection;
        deserializer = buddyListResponseDeserializerFactory.create(connection);
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
        
    }
    
    private void updatePresence(FriendPresence presence) {
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.UPDATE));
    }
    
    private void addPresence(FacebookFriend presence) {
        Friend friend = presence.getFriend();
        connection.userAvailable(friend.getId(), presence);
        friendManager.addAvailableFriend(friend);
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.ADDED));
    }
    
    private void removePresence(FriendPresence presence) {
        Friend friend = presence.getFriend();
        connection.userUnavailable(friend.getId());
        friendManager.removeAvailableFriend(friend);
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.REMOVED));
    }
}