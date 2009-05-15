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

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJsonRestClient;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

public class LiveMessageAuthTokenTransport implements FeatureTransport<AuthToken>, LiveMessageHandler {
    
    private final Provider<String> apiKey;
    private final FacebookFriendConnection connection;
    
    @AssistedInject
    LiveMessageAuthTokenTransport(@Assisted FacebookFriendConnection connection,
                                  @Named("facebookApiKey") Provider<String> apiKey) {
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
        return "auth-token";
    }

    @Override
    public void handle(JSONObject message) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
    
    @Override
    public void sendFeature(FriendPresence presence, AuthToken localFeature) throws FriendException {
        Map<String, String> message = new HashMap<String, String>();
        message.put("from", connection.getUID());
        message.put("auth-token", StringUtils.toUTF8String(Base64.encodeBase64(localFeature.getToken())));
        connection.sendLiveMessage(presence, getMessageType(), new JSONObject(message));
    }
}
