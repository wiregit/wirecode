package org.limewire.facebook.service;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.address.AddressEvent;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJsonRestClient;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class AddressSender implements EventListener<AddressEvent> {
    private final Provider<String> apiKey;
    private final FacebookFriendConnection connection;
    private final Map<String, String> friends = new HashMap<String, String>();

    @Inject
    AddressSender(@Named("facebookApiKey") Provider<String> apiKey,
                  FacebookFriendConnection connection) {
        this.apiKey = apiKey;
        this.connection = connection;
    }
    
    @Inject
    public void register(@Named("available") ListenerSupport<FriendEvent> availableFriends) {
        availableFriends.addListener(new EventListener<FriendEvent>() {
            @Override
            public void handleEvent(FriendEvent event) {                
                FacebookJsonRestClient client = new FacebookJsonRestClient(apiKey.get(),
                connection.getSecret(), connection.getSession());
                try {
                    JSONArray friendsArray = (JSONArray)client.friends_getAppUsers();
                    if(friendsArray != null) {
                        for(int i = 0; i < friendsArray.length(); i++) {
                            friends.put(friendsArray.getString(i), friendsArray.getString(i));
                        }
                    }
                } catch (FacebookException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(event.getType() == FriendEvent.Type.ADDED) {
                    if(friends.containsKey(event.getData().getId())) {
                        //FacebookJsonRestClient client = new FacebookJsonRestClient(apiKey.get(),
                        //connection.getSecret(), connection.getSession());
                        try {
                            Map<String, String> message = new HashMap<String, String>();
                            message.put("from", connection.getUID());
                            client.liveMessage_send(Long.parseLong(event.getData().getId()), "disco_info",
                                    new JSONObject(message));
                        } catch (FacebookException e) {
                            throw new RuntimeException(e);
                        }  
                    }
                }
            }
        });
    }
    
    public void start() {
//        FacebookJsonRestClient client = new FacebookJsonRestClient(apiKey.get(),
//                connection.getSecret(), connection.getSession());
//        try {
//            JSONArray friendsArray = (JSONArray)client.friends_getAppUsers();
//            if(friendsArray != null) {
//                for(int i = 0; i < friendsArray.length(); i++) {
//                    friends.put(friendsArray.getString(i), friendsArray.getString(i));
//                }
//            }
//        } catch (FacebookException e) {
//            throw new RuntimeException(e);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }
    
    @Override
    public void handleEvent(AddressEvent event) {
        if(event.getType() == AddressEvent.Type.ADDRESS_CHANGED) {
            FacebookJsonRestClient client = new FacebookJsonRestClient(apiKey.get(), "");
            try {
                client.liveMessage_send(new Long(0), "", null);
            } catch (FacebookException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
