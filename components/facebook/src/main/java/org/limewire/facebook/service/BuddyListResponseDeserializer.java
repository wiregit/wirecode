package org.limewire.facebook.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.io.Address;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;

import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

@Singleton
public class BuddyListResponseDeserializer {
    
    private final FacebookFriendConnection connection;
    private final LiveMessageAddressTransportFactory addressTransportFactory;
    private final LiveMessageAuthTokenTransportFactory authTokenTransportFactory;
    private final EventBroadcaster<FeatureEvent> featureBroadcaster;

    @AssistedInject
    BuddyListResponseDeserializer(@Assisted FacebookFriendConnection connection,
                                  LiveMessageAddressTransportFactory addressTransportFactory,
                                  LiveMessageAuthTokenTransportFactory authTokenTransportFactory,
                                  EventMulticaster<FeatureEvent> featureBroadcaster) {
        this.connection = connection;
        this.addressTransportFactory = addressTransportFactory;
        this.authTokenTransportFactory = authTokenTransportFactory;
        this.featureBroadcaster = featureBroadcaster;
    }
    
    public Map<String, FacebookFriend> deserialize(String response) throws JSONException {
        if(response == null) {
			return Collections.emptyMap();
        }
		String prefix = "for (;;);";
		if(response.startsWith(prefix)) {
			response = response.substring(prefix.length());
        }
		
		JSONObject respObjs =new JSONObject(response);
		if(respObjs == null) {
			return Collections.emptyMap();
        }
		if(respObjs.get("error") != null){
			if(respObjs.getInt("error") == 0) {
				//no error
				try{
					JSONObject payload = (JSONObject) respObjs.get("payload");
					if(payload != null){
						JSONObject buddyList = (JSONObject) payload.get("buddy_list");
						if(buddyList != null){
							return deserialize(buddyList);
						}
					}
				} catch(ClassCastException cce){
					cce.printStackTrace();
					//for (;;);{"error":0,"errorSummary":"","errorDescription":"No error.","payload":[],"bootload":[{"name":"js\/common.js.pkg.php","type":"js","src":"http:\/\/static.ak.fbcdn.net\/rsrc.php\/pkg\/60\/106715\/js\/common.js.pkg.php"}]}
					//"payload":[]
					//not "{}"
					//we do nothing
				}
			}
		}
        return null;
    }
    
    private Map<String, FacebookFriend> deserialize(JSONObject buddyList) throws JSONException {
		boolean listChanged = (Boolean)buddyList.get("listChanged");
		Number availableCount = (Number)buddyList.get("availableCount");
		Map<String, FacebookFriend> onlineFriends = new HashMap<String, FacebookFriend>(availableCount.intValue());
		
		JSONObject nowAvailableList = (JSONObject) buddyList.get("nowAvailableList");
		JSONObject userInfos = (JSONObject) buddyList.get("userInfos");
		
		if(nowAvailableList == null)
			return Collections.emptyMap();
		
		Iterator<String> it = nowAvailableList.keys();
		while(it.hasNext()){
			String key = it.next();
			JSONObject user = (JSONObject) userInfos.get(key);
			FacebookFriend friend = new FacebookFriend(key, user, connection.getConfiguration(), featureBroadcaster);
            friend.addTransport(Address.class, addressTransportFactory.create(connection));
            friend.addTransport(AuthToken.class, authTokenTransportFactory.create(connection));
			onlineFriends.put(friend.getId(), friend);
			//Launcher.getChatroomAnyway(key).setRoomName(fu.name);
		}
        return onlineFriends;
	}
}
