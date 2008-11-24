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
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.client.impl.XMPPAuthenticator;
import org.limewire.xmpp.client.impl.XMPPConnectionImpl;

public class AuthTokenIQListener implements PacketListener {
    private static final Log LOG = LogFactory.getLog(AuthTokenIQListener.class);

    private final XMPPConnectionImpl connection;
    private final XMPPAuthenticator authenticator;
    private final Map<String, byte []> pendingAuthTokens;
    private final ListenerSupport<FeatureEvent> featureSupport;
    private final EventListener<FeatureEvent> featureListener;

    public AuthTokenIQListener(XMPPConnectionImpl connection,
                               XMPPAuthenticator authenticator,
                               ListenerSupport<FeatureEvent> featureSupport) {
        this.connection = connection;
        this.authenticator = authenticator;
        this.pendingAuthTokens = new HashMap<String, byte[]>();
        this.featureSupport = featureSupport;
        this.featureListener = new FeatureListener();
        featureSupport.addListener(featureListener);
    }
    
    public void dispose() {
        featureSupport.removeListener(featureListener);
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
            User user = connection.getUser(StringUtils.parseBareAddress(iq.getFrom()));
            if (user != null) {
                FriendPresence presence = user.getFriendPresences().get(iq.getFrom());
                if(presence != null) {
                    if(iq.getAuthToken() != null) {
                        if(LOG.isDebugEnabled()) {
                            try {
                                LOG.debug("updating auth token on presence " + presence.getPresenceId() + " to " + new String(Base64.encodeBase64(iq.getAuthToken()), "UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }
                        presence.addFeature(new AuthTokenFeature(iq.getAuthToken()));
                    }
                }  else {
                    LOG.debugf("auth token {0} for presence {1} is pending", iq.getAuthToken(), iq.getFrom());
                    pendingAuthTokens.put(iq.getFrom(), iq.getAuthToken());
                }
            }
        }
    }

    private void sendResult(FriendPresence presence) {
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

    private class FeatureListener implements EventListener<FeatureEvent> {

        @BlockingEvent
        public void handleEvent(FeatureEvent featureEvent) {
            FriendPresence presence = featureEvent.getSource();
            String jid = presence.getPresenceId();

            switch(featureEvent.getType()) {
                case ADDED:
                    if (featureEvent.getData().getID().equals(LimewireFeature.ID)) {
                        synchronized (AuthTokenIQListener.this) {
                            sendResult(presence);
                            if (pendingAuthTokens.containsKey(jid)) {
                                byte[] authToken = pendingAuthTokens.remove(jid);
                                if (LOG.isDebugEnabled()) {
                                    try {
                                        LOG.debug("updating auth token on presence " + jid + " to " + new String(Base64.encodeBase64(authToken), "UTF-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        LOG.error(e.getMessage(), e);
                                    }
                                }
                                presence.addFeature(new AuthTokenFeature(authToken));
                            }
                        }
                    }
                    break;
                case REMOVED:
                    synchronized (AuthTokenIQListener.this) {
                        pendingAuthTokens.remove(jid);
                    }
                    break;
            }
        }
    }
}
