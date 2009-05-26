package org.limewire.xmpp.client.impl.messages.authtoken;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.core.api.friend.impl.DefaultFriendAuthenticator;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.XMPPFriend;
import org.limewire.xmpp.client.impl.XMPPConnectionImpl;

public class AuthTokenIQListener implements PacketListener {
    private static final Log LOG = LogFactory.getLog(AuthTokenIQListener.class);

    private final XMPPConnectionImpl connection;
    private final DefaultFriendAuthenticator authenticator;
    private final Map<String, byte []> pendingAuthTokens;

    public AuthTokenIQListener(XMPPConnectionImpl connection,
                               DefaultFriendAuthenticator authenticator,
                               FeatureRegistry featureRegistry) {
        this.connection = connection;
        this.authenticator = authenticator;
        this.pendingAuthTokens = new HashMap<String, byte[]>();
        new AuthTokenFeatureInitializer().register(featureRegistry);
    }

    public void processPacket(Packet packet) {
        AuthTokenIQ iq = (AuthTokenIQ)packet;
        if(iq.getType().equals(IQ.Type.SET)) {
            handleSet(iq);
        }
    }

    private void handleSet(AuthTokenIQ iq) {
        handleAuthTokenUpdate(iq);
    }

    private void handleAuthTokenUpdate(AuthTokenIQ iq) {
        synchronized (this) {
            XMPPFriend user = connection.getUser(StringUtils.parseBareAddress(iq.getFrom()));
            if (user != null) {
                FriendPresence presence = user.getFriendPresences().get(iq.getFrom());
                if(presence != null) {
                    LOG.debugf("updating auth token on presence {0} to {1}", presence, iq);
                    presence.addFeature(new AuthTokenFeature(iq.getAuthToken()));
                }  else {
                    LOG.debugf("auth token {0} for presence {1} is pending", iq, iq.getFrom());
                    pendingAuthTokens.put(iq.getFrom(), iq.getAuthToken());
                }
            }
        }
    }

    private void sendResult(FriendPresence presence) throws FriendException {
        byte [] authToken = authenticator.getAuthToken(StringUtils.parseBareAddress(presence.getPresenceId())).getBytes(Charset.forName("UTF-8"));
        AuthTokenIQ queryResult = new AuthTokenIQ(authToken);
        queryResult.setTo(presence.getPresenceId());
        queryResult.setFrom(connection.getLocalJid());
        queryResult.setType(IQ.Type.SET);
        connection.sendPacket(queryResult);
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof AuthTokenIQ;
            }
        };
    }

    private class AuthTokenFeatureInitializer implements FeatureInitializer {
        @Override
        public void register(FeatureRegistry registry) {
            registry.add(AuthTokenFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            synchronized (AuthTokenIQListener.this) {
                try {
                    sendResult(friendPresence);
                } catch (FriendException e) {
                    LOG.debugf(e, "couldn't send auth token to {0} " + friendPresence);
                }
                if (pendingAuthTokens.containsKey(friendPresence.getPresenceId())) {
                    byte[] authToken = pendingAuthTokens.remove(friendPresence.getPresenceId());
                    if (LOG.isDebugEnabled()) {
                        try {
                            LOG.debug("updating auth token on presence " + friendPresence.getPresenceId() + " to " + new String(Base64.encodeBase64(authToken), "UTF-8"));
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