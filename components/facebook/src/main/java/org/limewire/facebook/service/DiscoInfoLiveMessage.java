package org.limewire.facebook.service;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJsonRestClient;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class DiscoInfoLiveMessage implements LiveMessageHandler {
    private final Provider<String> apiKey;
    private final FacebookFriendConnection connection;
    private final Map<String, String> friends = new HashMap<String, String>();

    @Inject
    DiscoInfoLiveMessage(@Named("facebookApiKey") Provider<String> apiKey,
                       FacebookFriendConnection connection) {
        this.apiKey = apiKey;
        this.connection = connection;
    }
    
    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(this);
    }

    @Override
    public String getMessageType() {
        return "disco-info";
    }

    @Override
    public void handle(JSONObject message) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
    
    @Inject
    public void register(@Named("available") ListenerSupport<FriendEvent> availableFriends,
                         ListenerSupport<XMPPConnectionEvent> connectionEventListenerSupport) {
        connectionEventListenerSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            public void handleEvent(XMPPConnectionEvent event) {
                if(event.getType() == XMPPConnectionEvent.Type.CONNECTED) {
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
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        
        availableFriends.addListener(new EventListener<FriendEvent>() {
            @Override
            public void handleEvent(FriendEvent event) {                
                if(event.getType() == FriendEvent.Type.ADDED) {
                    FacebookJsonRestClient client = new FacebookJsonRestClient(apiKey.get(), connection.getSecret(), connection.getSession());
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
}
