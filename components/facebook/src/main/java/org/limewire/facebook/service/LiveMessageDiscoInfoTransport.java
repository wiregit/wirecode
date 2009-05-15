package org.limewire.facebook.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;

import com.google.code.facebookapi.FacebookException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

public class LiveMessageDiscoInfoTransport implements LiveMessageHandler {

    private final FacebookFriendConnection connection;
    private final FeatureRegistry featureRegistry;
    private final Map<String, String> friends = new HashMap<String, String>();

    @AssistedInject
    LiveMessageDiscoInfoTransport(@Assisted FacebookFriendConnection connection,
            FeatureRegistry featureRegistry) {
        this.connection = connection;
        this.featureRegistry = featureRegistry; // TODO FeatureRegistry is a global
        // TODO singleton, needs ot be per FriendConnection
    }

    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(this);
    }

    @Override
    public String getMessageType() {
        return "disco_info";
    }

    @Override
    public void handle(JSONObject message) throws JSONException {  
        try {
            if(message.getString("event_name").equals("disco_info_response")) {
                String from = message.getString("from");
                if(from != null) {
                    FacebookFriend friend = connection.getUser(from);
                    if(friend != null) {
                        JSONArray features = message.getJSONArray("features");     
                        for(int i = 0; i < features.length(); i++) {
                            String feature = features.getString(i);
                            FeatureInitializer initializer = featureRegistry.get(new URI(feature));
                            if(initializer != null) {
                                initializer.initializeFeature(friend);
                            }
                        }
                    }
                }                           
            } else if(message.getString("event_name").equals(getMessageType())) {
                String from  = message.getString("from");
                if(from != null) {
                    FacebookFriend friend = connection.getUser(from);
                    if(friend != null) {
                        // TODO replace with ServiceDiscoveryManager like thing
                        List<String> supported = new ArrayList<String>();
                        for(URI feature : featureRegistry) {
                            supported.add(feature.toASCIIString());
                        }
                        JSONObject response = new JSONObject("response");
                        response.put("from", connection.getUID());
                        response.put("features", supported);
                        connection.sendLiveMessage(friend.getActivePresence(), "disco_info_response",
                                        response);
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new JSONException(e);
        } catch (FriendException e) {
            throw new JSONException(e);
        }
    }
    
    @Inject
    public void register(@Named("available") ListenerSupport<FriendEvent> availableFriends,
                         ListenerSupport<FriendConnectionEvent> connectionEventListenerSupport) {
        connectionEventListenerSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                if(event.getType() == FriendConnectionEvent.Type.CONNECTED) {
                    try {
                        JSONArray friendsArray = (JSONArray)connection.getClient().friends_getAppUsers();
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
                    if(friends.containsKey(event.getData().getId())) {
                        //FacebookJsonRestClient client = new FacebookJsonRestClient(apiKey.get(),
                        //connection.getSecret(), connection.getSession());
                        try {
                            Map<String, String> message = new HashMap<String, String>();
                            message.put("from", connection.getUID());
                            connection.sendLiveMessage(event.getData().getActivePresence(), getMessageType(),
                                    new JSONObject(message));
                        } catch (FriendException e) {
                            throw new RuntimeException(e);
                        }  
                    }
                }
            }
        });
    }
}
