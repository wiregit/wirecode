package org.limewire.facebook.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.MutableFriendManager;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.io.Address;

import com.google.code.facebookapi.FacebookException;
import com.google.inject.Inject;
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
    
    private final ConnectionListener connectionListener = new ConnectionListener();

    private final EventBroadcaster<FeatureEvent> featureBroadcaster;
    private final LiveMessageAddressTransport addressTransport;
    private final LiveMessageAuthTokenTransport authTokenTransport;

    @AssistedInject
    PresenceListener(@Assisted FacebookFriendConnection connection,
                     MutableFriendManager friendManager,
                     EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster,                     
                     BuddyListResponseDeserializerFactory buddyListResponseDeserializerFactory,
                     EventBroadcaster<FeatureEvent> featureBroadcaster,
                     LiveMessageAddressTransportFactory addressTransportFactory,
                     LiveMessageAuthTokenTransportFactory authTokenTransportFactory) {
        this.friendManager = friendManager;
        this.friendPresenceBroadcaster = friendPresenceBroadcaster;
        this.connection = connection;
        this.featureBroadcaster = featureBroadcaster;
        addressTransport = addressTransportFactory.create(this.connection);
        authTokenTransport = authTokenTransportFactory.create(this.connection);
        deserializer = buddyListResponseDeserializerFactory.create(connection, addressTransport, authTokenTransport);
    }
    
    @Inject
    void register(ListenerSupport<FriendConnectionEvent> listenerSupport) {
        listenerSupport.addListener(connectionListener);
    }
    
    private void fetchFriends() {
        try {
            JSONArray friends = connection.getClient().friends_get();
            List<Long> friendIds = new ArrayList<Long>(friends.length());
            for (int i = 0; i < friends.length(); i++) {
                friendIds.add(friends.getLong(i));
            }
            JSONArray users = (JSONArray) connection.getClient().users_getInfo(friendIds, new HashSet<CharSequence>(Arrays.asList("uid", "first_name", "name")));
            LOG.debugf("all friends: {0}", users);
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                FacebookFriend friend = new FacebookFriend(user.getString("uid"), user, connection.getConfiguration(),
                        featureBroadcaster);
                friend.addTransport(Address.class, addressTransport);
                friend.addTransport(AuthToken.class, authTokenTransport);
                if (connection.getUser(friend.getId()) == null) {
                    LOG.debugf("adding {0}", friend);
                    addPresence(friend);
                } else {
                    LOG.debugf("not adding {0}", friend);
                }
            }
        } catch (FacebookException e) {
            LOG.debug("friend error", e);
        } catch (JSONException e) {
            LOG.debug("json error", e);
        }
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
                    removePresence(connection.getUser(friend.getId()));
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
    
    private class ConnectionListener implements EventListener<FriendConnectionEvent> {

        @Override
        public void handleEvent(FriendConnectionEvent event) {
            switch(event.getType()) {
            case DISCONNECTED:
                for(Friend user : event.getSource().getUsers()) {
                    friendManager.removeKnownFriend(user, false);
                }
                break;
            case CONNECTED:
                LOG.debug("connect event");
                fetchFriends();
                break;
            }
        }
        
    }
}