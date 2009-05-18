package org.limewire.facebook.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class LiveMessageAuthTokenTransport implements FeatureTransport<AuthToken>, LiveMessageHandler {
    
    private static final String TYPE = "auth-token";
    
    private final FacebookFriendConnection connection;
    
    @AssistedInject
    LiveMessageAuthTokenTransport(@Assisted FacebookFriendConnection connection) {
        this.connection = connection;
    }
    
    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }

    @Override
    public void handle(String messageType, JSONObject message) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
    
    @Override
    public void sendFeature(FriendPresence presence, AuthToken localFeature) throws FriendException {
        Map<String, String> message = new HashMap<String, String>();
        message.put("from", connection.getUID());
        message.put("auth-token", StringUtils.toUTF8String(Base64.encodeBase64(localFeature.getToken())));
        connection.sendLiveMessage(presence, TYPE, message);
    }
}
