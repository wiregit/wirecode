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
import org.limewire.core.api.friend.FriendPresence;
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

    private void handleDiscInfoResponse(JSONObject message) throws JSONException, URISyntaxException {
        JSONArray features = message.getJSONArray("features");     
        String from = message.getString("from");
        FacebookFriend friend = connection.getFriend(from);
        if (friend == null) {
            LOG.debugf("no friend for id {0}", from);
            return;
        }
        FriendPresence presence = friend.getFacebookPresence();
        initializePresenceFeatures(presence, features);
    }
    
    private void handleDiscInfoRequest(JSONObject message) throws JSONException, FriendException {
        String friendId = message.getString("from");
        FacebookFriend friend = connection.getFriend(friendId);
        if (friend == null) {
            LOG.debugf("disc info from non-friend: {0}", friendId);
            return;
        }
        // this is the first unconditional message received, so the friend
        // might not be in list of online friends yet, mark friend as available
        // to obtain presence
        FacebookFriendPresence presence = connection.setAvailable(friend);
        List<String> supported = new ArrayList<String>();
        for(URI feature : featureRegistry) {
            supported.add(feature.toASCIIString());
        }
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("from", connection.getUID());
        response.put("features", supported);
        connection.sendLiveMessage(presence, RESPONSE_TYPE, response);   
    }
    
    @Override
    public void handle(String messageType, JSONObject message) throws JSONException {
        try {
            if(messageType.equals(RESPONSE_TYPE)) {
                handleDiscInfoResponse(message);
            } else if(messageType.equals(REQUEST_TYPE)) {                
                handleDiscInfoRequest(message);
            }
        } catch (URISyntaxException e) {
            throw new JSONException(e);
        } catch (FriendException e) {
            throw new JSONException(e);
        }
    }
    
    private void initializePresenceFeatures(FriendPresence presence, JSONArray features) throws JSONException, URISyntaxException {
        for(int i = 0; i < features.length(); i++) {
            String feature = features.getString(i);
            FeatureInitializer initializer = featureRegistry.get(new URI(feature));
            if (initializer != null) {
                initializer.initializeFeature(presence);
            }
        }
    }
    
    @Inject
    public void register(@Named("available") ListenerSupport<FriendEvent> availableFriends) {
        availableFriends.addListener(new EventListener<FriendEvent>() {
            @Override
            public void handleEvent(FriendEvent event) {  
                if(event.getType() != FriendEvent.Type.ADDED) {
                    return;
                }
                Friend friend = event.getData();
                if (!(friend instanceof FacebookFriend)) {
                    return;
                }
                FacebookFriend facebookFriend = (FacebookFriend)friend;
                if (!facebookFriend.hasLimeWireAppInstalled()) {
                    LOG.debugf("not a limewire friend: {0}", friend);
                    return;
                }
                FacebookFriendPresence presence = facebookFriend.getFacebookPresence();
                try {
                    Map<String, String> message = new HashMap<String, String>();
                    message.put("from", connection.getUID());
                    connection.sendLiveMessage(presence, REQUEST_TYPE, message);
                } catch (FriendException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
