package org.limewire.facebook.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.MutableFriendManager;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.io.Address;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

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

    private LiveMessageAddressTransport addressTransport;

    private LiveMessageAuthTokenTransport authTokenTransport;
    
    @AssistedInject
    PresenceListener(@Assisted FacebookFriendConnection connection,
                     MutableFriendManager friendManager,
                     EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster,                     
                     BuddyListResponseDeserializerFactory buddyListResponseDeserializerFactory,
                     LiveMessageAddressTransportFactory liveAddressTransportFactory,
                     LiveMessageAuthTokenTransportFactory liveAuthTokenTransportFactory) {
        this.friendManager = friendManager;
        this.friendPresenceBroadcaster = friendPresenceBroadcaster;
        this.connection = connection;
        this.deserializer = buddyListResponseDeserializerFactory.create(connection);
        this.addressTransport = liveAddressTransportFactory.create(connection);
        authTokenTransport = liveAuthTokenTransportFactory.create(connection);
    }
    
    @Inject
    void register(ListenerSupport<FriendConnectionEvent> listenerSupport) {
        listenerSupport.addListener(connectionListener);
    }
    
    /**
     * Fetches all friends and adds them as known friends.
     */
    private void fetchAllFriends() {
        try {
            JSONArray friends = connection.getClient().friends_get();
            List<Long> friendIds = new ArrayList<Long>(friends.length());
            for (int i = 0; i < friends.length(); i++) {
                friendIds.add(friends.getLong(i));
            }
            JSONArray users = (JSONArray) connection.getClient().users_getInfo(friendIds, new HashSet<CharSequence>(Arrays.asList("uid", "first_name", "name", "status")));
            Set<String> limeWireFriends = fetchLimeWireFriends();
            LOG.debugf("all friends: {0}", users);
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                String id = user.getString("uid");
                FacebookFriend friend = new FacebookFriend(id, user,
                        connection.getNetwork(), limeWireFriends.contains(id));
                LOG.debugf("adding {0}", friend);
                addFriend(friend);
            }
        } catch (FacebookException e) {
            LOG.debug("friend error", e);
        } catch (JSONException e) {
            LOG.debug("json error", e);
        }
    }
    
    /**
     * Fetches friend ids that have the LimeWire application installed
     * and marks the existing friends as LimeWire capable.
     */
    private Set<String> fetchLimeWireFriends() {
        JSONArray limeWireFriendIds;
        try {
            limeWireFriendIds = (JSONArray)connection.getClient().friends_getAppUsers();
            LOG.debugf("limewire friends: {0}", limeWireFriendIds);
            Set<String> limeWireIds = new HashSet<String>(limeWireFriendIds.length());
            for (int i = 0; i < limeWireFriendIds.length(); i++) {
                limeWireIds.add(limeWireFriendIds.getString(i));
            }
            return limeWireIds;
        }
        catch (FacebookException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void run() {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("buddy_list", "1"));
        nvps.add(new BasicNameValuePair("notifications", "1"));
        nvps.add(new BasicNameValuePair("force_render", "true"));
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

            Map<String, FacebookFriendPresence> onlineFriends = deserializer.deserialize(responseStr);
            for (FacebookFriend friend : connection.getFriends()) {
                FacebookFriendPresence presence = onlineFriends.get(friend.getId());
                if (presence != null) {
                    updatePresence(friend, presence);
                } else {
                    removePresence(friend);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    private void addFriend(FacebookFriend friend) {
        connection.addFriend(friend);
        friendManager.addKnownFriend(friend);
    }
    
    private void updatePresence(FacebookFriend friend, FacebookFriendPresence presence) {
        FacebookFriendPresence oldPresences = friend.getFacebookPresence();
        if (oldPresences == null) {
            LOG.debugf("new friend is available: {0}", friend);
            if (friend.hasLimeWireAppInstalled()) {
                addTransports(presence);
            }
            friend.setPresence(presence);
            friendManager.addAvailableFriend(friend);
            friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.ADDED));
        } else {
            LOG.debugf("friend already available: {0}", friend);
        }
    } 
    
    private void addTransports(FacebookFriendPresence presence) {
        presence.addTransport(Address.class, addressTransport);
        presence.addTransport(AuthToken.class, authTokenTransport);
    }

    private void removePresence(FacebookFriend friend) {
        FacebookFriendPresence presence = friend.getFacebookPresence();
        if (presence != null) {
            LOG.debugf("removing offline friend {0}", friend);
            friendManager.removeAvailableFriend(friend);
            friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.REMOVED));
        } else {
            LOG.debugf("offline friend already removed: {0}", friend);
        }
    }
    
    private class ConnectionListener implements EventListener<FriendConnectionEvent> {
        @Override
        // TODO make this a @BlockingEvent
        public void handleEvent(FriendConnectionEvent event) {
            switch(event.getType()) {
            case CONNECTED:
                LOG.debug("connect event");
                fetchAllFriends();
                break;
            }
        }
    }
}