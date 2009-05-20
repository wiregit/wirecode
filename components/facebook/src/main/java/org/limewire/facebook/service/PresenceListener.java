package org.limewire.facebook.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.code.facebookapi.FacebookException;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

@Singleton
public class PresenceListener implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(PresenceListener.class);
    
    private final FacebookFriendConnection connection;
    private final BuddyListResponseDeserializer deserializer = new BuddyListResponseDeserializer();
    
    private final AtomicBoolean firstRun = new AtomicBoolean(true);
    
    @AssistedInject
    PresenceListener(@Assisted FacebookFriendConnection connection) {
        this.connection = connection;
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
                connection.addKnownFriend(friend);
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
        if (firstRun.compareAndSet(true, false)) {
            fetchAllFriends();
        }
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

            Set<String> onlineFriendIds = deserializer.parseOnlineFriendIds(responseStr);
            for (FacebookFriend friend : connection.getFriends()) {
                if (onlineFriendIds.contains(friend.getId())) {
                    connection.setAvailable(friend);
                } else {
                    connection.removePresence(friend);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
 
}