package org.limewire.core.api.friend.feature.features;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.core.api.friend.impl.DefaultFriendAuthenticator;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPFriend;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AuthTokenHandler implements FeatureTransport.Handler<AuthToken>{
    
    private static final Log LOG = LogFactory.getLog(AuthTokenHandler.class);
    
    private final Map<String, AuthToken> pendingAuthTokens;
    private final DefaultFriendAuthenticator authenticator;
    private final Set<FriendConnection> connections;
    
    @Inject
    AuthTokenHandler(DefaultFriendAuthenticator authenticator,
                     FeatureRegistry featureRegistry) {
        this.authenticator = authenticator;
        this.connections = new HashSet<FriendConnection>();
        this.pendingAuthTokens = new HashMap<String, AuthToken>();
        new AuthTokenFeatureInitializer().register(featureRegistry);
    }
    
    @Inject
    void register(ListenerSupport<XMPPConnectionEvent> connectionEventListenerSupport) {
        connectionEventListenerSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            public void handleEvent(XMPPConnectionEvent event) {
                switch (event.getType()) {
                    case CONNECTED:
                        connections.add(event.getSource()); 
                        break;
                    case DISCONNECTED:
                        connections.remove(event.getSource());
                }
            }
        });
    }
    
    @Override
    public void featureReceived(String from, final AuthToken feature) {
        for(FriendConnection connection : connections) {
            synchronized (this) {
                XMPPFriend user = connection.getUser(StringUtils.parseBareAddress(from));
                if (user != null) {
                    FriendPresence presence = user.getFriendPresences().get(from);
                    if(presence != null) {
                        LOG.debugf("updating auth token on presence {0} to {1}", presence, feature);
                        presence.addFeature(new AuthTokenFeature(feature));
                    }  else {
                        LOG.debugf("auth token {0} for presence {1} is pending", feature, from);
                        pendingAuthTokens.put(from, feature);
                    }
                }
            }
        }
    }
    
    private class AuthTokenFeatureInitializer implements FeatureInitializer {

        @Override
        public void register(FeatureRegistry registry) {
            registry.add(AuthTokenFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            synchronized (AuthTokenHandler.this) {
                try {
                    final byte [] authToken = authenticator.getAuthToken(StringUtils.parseBareAddress(friendPresence.getPresenceId())).getBytes(Charset.forName("UTF-8"));
                    FeatureTransport<AuthToken> transport = friendPresence.getTransport(AuthTokenFeature.class);
                    transport.sendFeature(friendPresence, new AuthToken() {
                        @Override
                        public byte[] getToken() {
                            return authToken;
                        }
                    });
                } catch (FriendException e) {
                    LOG.debugf(e, "couldn't send auth token to {0} " + friendPresence);
                }
                if (pendingAuthTokens.containsKey(friendPresence.getPresenceId())) {
                    final AuthToken authToken = pendingAuthTokens.remove(friendPresence.getPresenceId());
                    if (LOG.isDebugEnabled()) {
                        try {
                            LOG.debug("updating auth token on presence " + friendPresence.getPresenceId() + " to " + new String(Base64.encodeBase64(authToken.getToken()), "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                    friendPresence.addFeature(new AuthTokenFeature(authToken));
                }
            }
        }

        @Override
        public void removeFeature(FriendPresence friendPresence) {
            friendPresence.removeFeature(AuthTokenFeature.ID);
        }
    }
}
