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

import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

@Singleton
public class PresenceListener implements Runnable {
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
        nvps.add(new BasicNameValuePair("post_form_id", postFormID));
        nvps.add(new BasicNameValuePair("user", connection.getUID()));
        
        try{
            String responseStr = connection.httpPOST("http://www.facebook.com", "/ajax/presence/update.php", nvps);
            //for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":{"buddy_list":{"listChanged":true,"availableCount":1,"nowAvailableList":{"UID1":{"i":false}},"wasAvailableIDs":[],"userInfos":{"UID1":{"name":"Buddy 1","firstName":"Buddy","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_default.gif","status":null,"statusTime":0,"statusTimeRel":""},"UID2":{"name":"Buddi 2","firstName":"Buddi","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_default.gif","status":null,"statusTime":0,"statusTimeRel":""}},"forcedRender":true},"time":1209560380000}}  
            //for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":{"time":1214626375000,"buddy_list":{"listChanged":true,"availableCount":1,"nowAvailableList":{},"wasAvailableIDs":[],"userInfos":{"1386786477":{"name":"\u5341\u4e00","firstName":"\u4e00","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_silhouette.gif","status":null,"statusTime":0,"statusTimeRel":""}},"forcedRender":null,"flMode":false,"flData":{}},"notifications":{"countNew":0,"count":1,"app_names":{"2356318349":"\u670b\u53cb"},"latest_notif":1214502420,"latest_read_notif":1214502420,"markup":"<div id=\"presence_no_notifications\" style=\"display:none\" class=\"no_notifications\">\u65e0\u65b0\u901a\u77e5\u3002<\/div><div class=\"notification clearfix notif_2356318349\" onmouseover=\"CSS.addClass(this, 'hover');\" onmouseout=\"CSS.removeClass(this, 'hover');\"><div class=\"icon\"><img src=\"http:\/\/static.ak.fbcdn.net\/images\/icons\/friend.gif?0:41046\" alt=\"\" \/><\/div><div class=\"notif_del\" onclick=\"return presenceNotifications.showHideDialog(this, 2356318349)\"><\/div><div class=\"body\"><a href=\"http:\/\/www.facebook.com\/profile.php?id=1190346972\"   >David Willer<\/a>\u63a5\u53d7\u4e86\u60a8\u7684\u670b\u53cb\u8bf7\u6c42\u3002 <span class=\"time\">\u661f\u671f\u56db<\/span><\/div><\/div>","inboxCount":"0"}},"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
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
