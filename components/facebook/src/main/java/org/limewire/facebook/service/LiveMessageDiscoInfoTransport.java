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
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

public class LiveMessageDiscoInfoTransport implements LiveMessageHandler {

    private static final Log LOG = LogFactory.getLog(LiveMessageAddressTransport.class);
    
    private static final String REQUEST_TYPE = "disc-info-request";
    private static final String RESPONSE_TYPE = "disc-info-response";
    
    private final FacebookFriendConnection connection;
    private final FeatureRegistry featureRegistry;
    private final Map<String, JSONArray> pendingPresences = new HashMap<String, JSONArray>();
    
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
        registry.register(REQUEST_TYPE, this);
        registry.register(RESPONSE_TYPE, this);
    }

    @Override
    public void handle(String messageType, JSONObject message) throws JSONException {
        synchronized (this) {
            try {
                if(messageType.equals(RESPONSE_TYPE)) {
                    JSONArray features = message.getJSONArray("features");     
                    String from = message.getString("from");
                    FacebookFriend friend = connection.getUser(from);
                    if(friend != null) {    
                        for(int i = 0; i < features.length(); i++) {
                            String feature = features.getString(i);
                            FeatureInitializer initializer = featureRegistry.get(new URI(feature));
                            if(initializer != null) {
                                initializer.initializeFeature(friend);
                            }
                        }
                    } else {
                        pendingPresences.put(from, features);
                    }
                } else if(messageType.equals(REQUEST_TYPE)) {                
                    Long from = message.getLong("from");
                    List<String> supported = new ArrayList<String>();
                    for(URI feature : featureRegistry) {
                        supported.add(feature.toASCIIString());
                    }
                    Map<String, Object> response = new HashMap<String, Object>();
                    response.put("from", connection.getUID());
                    response.put("features", supported);
                    connection.sendLiveMessage(from, RESPONSE_TYPE, response);
                }
            } catch (URISyntaxException e) {
                throw new JSONException(e);
            } catch (FriendException e) {
                throw new JSONException(e);
            }
        }
    }
    
    @Inject
    public void register(@Named("available") ListenerSupport<FriendEvent> availableFriends) {
        availableFriends.addListener(new EventListener<FriendEvent>() {
            @Override
            public void handleEvent(FriendEvent event) {  
                synchronized (LiveMessageDiscoInfoTransport.this) {
                    if(event.getType() == FriendEvent.Type.ADDED) {
                        Friend friend = event.getData();
                        if (friend instanceof FacebookFriend) {
                            FacebookFriend facebookFriend = (FacebookFriend)friend;
                            try {
                                Map<String, String> message = new HashMap<String, String>();
                                message.put("from", connection.getUID());
                                connection.sendLiveMessage(facebookFriend, REQUEST_TYPE,
                                        message);
                                
                                if(pendingPresences.containsKey(facebookFriend.getId())) {
                                    JSONArray features = pendingPresences.remove(facebookFriend.getId());
                                    for(int i = 0; i < features.length(); i++) {
                                        String feature = features.getString(i);
                                        FeatureInitializer initializer = featureRegistry.get(new URI(feature));
                                        if(initializer != null) {
                                            initializer.initializeFeature(facebookFriend);
                                        }
                                    }
                                }
                            } catch (FriendException e) {
                                throw new RuntimeException(e);
                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        });
    }
}
