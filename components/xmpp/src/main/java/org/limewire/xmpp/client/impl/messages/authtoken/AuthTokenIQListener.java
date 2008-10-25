package org.limewire.xmpp.client.impl.messages.authtoken;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.client.impl.LimePresenceImpl;
import org.limewire.xmpp.client.impl.XMPPAuthenticator;
import org.xmlpull.v1.XmlPullParserException;

public class AuthTokenIQListener implements PacketListener {
    private static final Log LOG = LogFactory.getLog(AuthTokenIQListener.class);

    private final XMPPConnection connection;
    private final XMPPAuthenticator authenticator;
     
    private final Map<String, LimePresenceImpl> limePresences = new HashMap<String, LimePresenceImpl>();
    private final RosterEventHandler rosterEventHandler;

    public AuthTokenIQListener(XMPPConnection connection,
            XMPPAuthenticator authenticator) {
        this.connection = connection;
        this.authenticator = authenticator;
        this.rosterEventHandler = new RosterEventHandler();
    }

    public void processPacket(Packet packet) {
        AuthTokenIQ iq = (AuthTokenIQ)packet;
        try {
            if(iq.getType().equals(IQ.Type.GET)) {
                handleGet(iq);
            } else if(iq.getType().equals(IQ.Type.RESULT)) {
                handleResult(iq);
            } else if(iq.getType().equals(IQ.Type.SET)) {
                handleSet(iq);
            } else if(iq.getType().equals(IQ.Type.ERROR)) {
                // TODO
                //handleError(iq);
            } else {
                // TODO
                //sendError(packet);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            // TODO
            //sendError(packet);
        } catch (XmlPullParserException e) {
            LOG.error(e.getMessage(), e);
            // TODO
            //sendError(packet);
        }
    }

    private void handleSet(AuthTokenIQ iq) {
        handleAuthTokenUpdate(iq);
    }
    
    private void handleResult(AuthTokenIQ addressIQ) {
        handleAuthTokenUpdate(addressIQ);
    }

    private void handleAuthTokenUpdate(AuthTokenIQ iq) {
        synchronized (this) {
            LimePresenceImpl presence = limePresences.get(iq.getFrom());
            if(presence != null) {
                if(iq.getAuthToken() != null) {
                    if(LOG.isDebugEnabled()) {
                        try {
                            LOG.debug("updating auth token on presence " + presence.getJID() + " to " + new String(Base64.encodeBase64(iq.getAuthToken()), "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                    presence.setAuthToken(iq.getAuthToken());
                }
            }
        }
    }
    
    private void handleGet(AuthTokenIQ packet) throws IOException, XmlPullParserException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling auth-token subscription from " + packet.getFrom());
        }
        sendResult(packet);
    }

    private void sendResult(AuthTokenIQ packet) {
        byte [] authToken = authenticator.getAuthToken(StringUtils.parseBareAddress(packet.getFrom())).getBytes(Charset.forName("UTF-8"));
        AuthTokenIQ queryResult = new AuthTokenIQ(authToken);
        queryResult.setTo(packet.getFrom());
        queryResult.setFrom(packet.getTo());
        queryResult.setPacketID(packet.getPacketID());
        queryResult.setType(IQ.Type.RESULT);
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
        user.addPresenceListener(new PresenceListener() {
            public void presenceChanged(Presence presence) {
                if(presence.getType().equals(Presence.Type.available)) {
                    if(presence instanceof LimePresence) {
                        synchronized (AuthTokenIQListener.this) {
                            limePresences.put(presence.getJID(), (LimePresenceImpl)presence);
                        }
                    }
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    synchronized (AuthTokenIQListener.this) {
                        limePresences.remove(presence.getJID());
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
