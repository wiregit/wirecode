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
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.client.impl.XMPPAuthenticator;
import org.limewire.xmpp.client.impl.XMPPConnectionImpl;

public class AuthTokenIQListener implements PacketListener {
    private static final Log LOG = LogFactory.getLog(AuthTokenIQListener.class);

    private final XMPPConnectionImpl connection;
    private final XMPPAuthenticator authenticator;
    private final RosterEventHandler rosterEventHandler;
    private Map<String, byte []> pendingAuthTokens;

    public AuthTokenIQListener(XMPPConnectionImpl connection,
                               XMPPAuthenticator authenticator) {
        this.connection = connection;
        this.authenticator = authenticator;
        this.rosterEventHandler = new RosterEventHandler();
        this.pendingAuthTokens = new HashMap<String, byte[]>();
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
                Presence presence = user.getPresences().get(iq.getFrom());
                if(presence != null) {
                    if(iq.getAuthToken() != null) {
                        if(LOG.isDebugEnabled()) {
                            try {
                                LOG.debug("updating auth token on presence " + presence.getJID() + " to " + new String(Base64.encodeBase64(iq.getAuthToken()), "UTF-8"));
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

    private void sendResult(Presence presence) {
        byte [] authToken = authenticator.getAuthToken(StringUtils.parseBareAddress(presence.getJID())).getBytes(Charset.forName("UTF-8"));
        AuthTokenIQ queryResult = new AuthTokenIQ(authToken);
        queryResult.setTo(presence.getJID());
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
    
    public EventListener<RosterEvent> getRosterListener() {
        return rosterEventHandler;
    }

    private void userAdded(User user) {
        user.addPresenceListener(new EventListener<PresenceEvent>() {
            public void handleEvent(PresenceEvent event) {
                final Presence presence = event.getSource();
                if(event.getType().equals(Presence.EventType.PRESENCE_NEW) &&
                        presence.getType().equals(Presence.Type.available)) {
                    presence.getFeatureListenerSupport().addListener(new EventListener<FeatureEvent>() {
                        
                        @BlockingEvent
                        public void handleEvent(FeatureEvent event) {
                            if(event.getType() == Feature.EventType.FEATURE_ADDED) {
                                if(event.getSource().getID().equals(LimewireFeature.ID)) {
                                    synchronized (AuthTokenIQListener.this) {
                                        sendResult(presence);
                                        if(pendingAuthTokens.containsKey(presence.getJID())) {
                                            byte [] authToken = pendingAuthTokens.remove(presence.getJID());
                                            if(LOG.isDebugEnabled()) {
                                                try {
                                                    LOG.debug("updating auth token on presence " + presence.getJID() + " to " + new String(Base64.encodeBase64(authToken), "UTF-8"));
                                                } catch (UnsupportedEncodingException e) {
                                                    LOG.error(e.getMessage(), e);
                                                }
                                            }
                                            presence.addFeature(new AuthTokenFeature(authToken));
                                        }
                                    }
                                }
                            }
                        }
                    });
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    synchronized (AuthTokenIQListener.this) {
                        pendingAuthTokens.remove(presence.getJID());    
                    }
                }
            }
        });
    }
    
    private class RosterEventHandler implements EventListener<RosterEvent> {
        public void handleEvent(RosterEvent event) {
            if(event.getType().equals(User.EventType.USER_ADDED)) {
                userAdded(event.getSource());
            } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
                userDeleted(event.getSource().getId());
            } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
                userUpdated(event.getSource());
            }
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void userUpdated(User user) {}

    @SuppressWarnings({"UnusedDeclaration"})
    private void userDeleted(String id) {}
}
