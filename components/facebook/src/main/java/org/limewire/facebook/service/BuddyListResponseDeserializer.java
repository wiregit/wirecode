package org.limewire.facebook.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

@Singleton
public class BuddyListResponseDeserializer {
    
    private static final Log LOG = LogFactory.getLog(BuddyListResponseDeserializer.class);
    
    private final FacebookFriendConnection connection;
    private final EventBroadcaster<FeatureEvent> featureBroadcaster;
    
    @AssistedInject
    BuddyListResponseDeserializer(@Assisted FacebookFriendConnection connection,
                                  EventMulticaster<FeatureEvent> featureBroadcaster) {
        this.connection = connection;
        this.featureBroadcaster = featureBroadcaster;
    }
    
    /**
     * 
     * @param response
     * @return empty map if the response could not be parsed
     * @throws JSONException
     */
    public Map<String, FacebookFriendPresence> deserialize(String response) throws JSONException {
        //for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":{"buddy_list":{"listChanged":true,"availableCount":1,"nowAvailableList":{"UID1":{"i":false}},"wasAvailableIDs":[],"userInfos":{"UID1":{"name":"Buddy 1","firstName":"Buddy","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_default.gif","status":null,"statusTime":0,"statusTimeRel":""},"UID2":{"name":"Buddi 2","firstName":"Buddi","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_default.gif","status":null,"statusTime":0,"statusTimeRel":""}},"forcedRender":true},"time":1209560380000}}  
        //for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":{"time":1214626375000,"buddy_list":{"listChanged":true,"availableCount":1,"nowAvailableList":{},"wasAvailableIDs":[],"userInfos":{"1386786477":{"name":"\u5341\u4e00","firstName":"\u4e00","thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_silhouette.gif","status":null,"statusTime":0,"statusTimeRel":""}},"forcedRender":null,"flMode":false,"flData":{}},"notifications":{"countNew":0,"count":1,"app_names":{"2356318349":"\u670b\u53cb"},"latest_notif":1214502420,"latest_read_notif":1214502420,"markup":"<div id=\"presence_no_notifications\" style=\"display:none\" class=\"no_notifications\">\u65e0\u65b0\u901a\u77e5\u3002<\/div><div class=\"notification clearfix notif_2356318349\" onmouseover=\"CSS.addClass(this, 'hover');\" onmouseout=\"CSS.removeClass(this, 'hover');\"><div class=\"icon\"><img src=\"http:\/\/static.ak.fbcdn.net\/images\/icons\/friend.gif?0:41046\" alt=\"\" \/><\/div><div class=\"notif_del\" onclick=\"return presenceNotifications.showHideDialog(this, 2356318349)\"><\/div><div class=\"body\"><a href=\"http:\/\/www.facebook.com\/profile.php?id=1190346972\"   >David Willer<\/a>\u63a5\u53d7\u4e86\u60a8\u7684\u670b\u53cb\u8bf7\u6c42\u3002 <span class=\"time\">\u661f\u671f\u56db<\/span><\/div><\/div>","inboxCount":"0"}},"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
        
        String prefix = "for (;;);";
		if(response.startsWith(prefix)) {
			response = response.substring(prefix.length());
        }
		JSONObject respObjs = new JSONObject(response);
		if(respObjs.get("error") != null){
			if(respObjs.getInt("error") == 0) {
			    JSONObject payload = (JSONObject) respObjs.get("payload");
			    if(payload != null){
			        JSONObject buddyList = (JSONObject) payload.get("buddy_list");
			        if(buddyList != null){
			            return deserialize(buddyList);
			        } else {
			            LOG.debug("no buddy_list");
			        }
			    } else {
			        LOG.debug("no payload");
			    }
			} else {
			    String errorDescription = respObjs.getString("errorDescription");
			    LOG.debugf("received error description: {0}", errorDescription);
			}
		}
        return Collections.emptyMap();
    }
    
    private Map<String, FacebookFriendPresence> deserialize(JSONObject buddyList) throws JSONException {
        JSONObject nowAvailableList = buddyList.getJSONObject("nowAvailableList");
		Map<String, FacebookFriendPresence> onlineFriends = new HashMap<String, FacebookFriendPresence>();
		
		if(nowAvailableList == null) {
		    LOG.debug("nowAvailableList not there");
			return Collections.emptyMap();
		}
		LOG.debugf("nowAvailableList: {0}", nowAvailableList);
	
		@SuppressWarnings("unchecked")
		Iterator<String> it = nowAvailableList.keys();
		while (it.hasNext()) {
		    String id = it.next();
			FacebookFriend friend = connection.getFriend(id);
			FacebookFriendPresence friendPresence = new FacebookFriendPresence(friend, featureBroadcaster);
            onlineFriends.put(friend.getId(), friendPresence);
		}
        return onlineFriends;
	}
}
