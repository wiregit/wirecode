package org.limewire.facebook.service.livemessage;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.core.api.friend.impl.AuthTokenImpl;
import org.limewire.util.StringUtils;
import org.limewire.facebook.service.FacebookFriendConnection;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class AuthTokenHandler implements LiveMessageHandler, FeatureTransport<AuthToken> {
    
    private static final String TYPE = "auth-token";
    
    private final FacebookFriendConnection connection;

    private final Handler<AuthToken> authTokenHandler;

    @AssistedInject
    AuthTokenHandler(@Assisted FacebookFriendConnection connection,
            FeatureTransport.Handler<AuthToken> authTokenHandler) {
        this.connection = connection;
        this.authTokenHandler = authTokenHandler;
    }

    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }

    @Override
    public void handle(String messageType, JSONObject message) {
        String from = message.optString("from", null);
        String authtoken = message.optString("auth-token", null);
        if (from != null && authtoken != null) {
            byte[] token = Base64.decodeBase64(StringUtils.toUTF8Bytes(authtoken));
            authTokenHandler.featureReceived(from, new AuthTokenImpl(token));
        }
    }
    
    @Override
    public void sendFeature(FriendPresence presence, AuthToken localFeature) throws FriendException {
        Map<String, String> message = new HashMap<String, String>();
        message.put("from", connection.getPresenceId());
        message.put("auth-token", StringUtils.toUTF8String(Base64.encodeBase64(localFeature.getToken())));
        connection.sendLiveMessage(presence, TYPE, message);
    }
}
