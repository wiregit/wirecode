package org.limewire.facebook.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.io.Address;

public class BuddyListResponseProcessor {
    public static Map<String, FacebookFriend> deserialize(String response) throws JSONException {
        if(response == null)
			return Collections.emptyMap();
		String prefix = "for (;;);";
		if(response.startsWith(prefix))
			response = response.substring(prefix.length());
		
		JSONObject respObjs =new JSONObject(response);
		if(respObjs == null)
			return Collections.emptyMap();
		if(respObjs.get("error") != null){
			/*kError_Global_ValidationError = 1346001,
			kError_Login_GenericError = 1348009,
			kError_Chat_NotAvailable = 1356002,
			kError_Chat_SendOtherNotAvailable = 1356003,
			kError_Async_NotLoggedIn = 1357001,
			kError_Async_LoginChanged = 1357003,
			kError_Async_CSRFCheckFailed = 1357004,
			kError_Chat_TooManyMessages = 1356008,
			kError_Platform_CallbackValidationFailure = 1349007,
			kError_Platform_ApplicationResponseInvalid = 1349008;*/

			if(respObjs.getInt("error") == 0){
				//no error
				try{
					JSONObject payload = (JSONObject) respObjs.get("payload");
					if(payload != null){
						JSONObject buddyList = (JSONObject) payload.get("buddy_list");
						if(buddyList != null){
							return deserialize(buddyList);
						}
					}
				}catch(ClassCastException cce){
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
    
    public static Map<String, FacebookFriend> deserialize(JSONObject buddyList) throws JSONException {
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
			FacebookFriend friend = new FacebookFriend(key, user);
            friend.addTransport(Address.class, new AddressLiveMessage(null, null, null));
			onlineFriends.put(friend.getId(), friend);
			//Launcher.getChatroomAnyway(key).setRoomName(fu.name);
		}
        return onlineFriends;
	}
}
