package org.limewire.xmpp.client.impl;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.FriendRequest;
import org.limewire.xmpp.api.client.FriendRequestDecisionHandler;
import org.limewire.xmpp.api.client.FriendRequestEvent;

/**
 * Handles presence subscriptions and unsubscriptions on an XMPP connection
 * (see RFC 3921 section 8). <code>FriendRequestEvent</code>s that require
 * decisions from the user are broadcast for interception by the UI, and the
 * results are passed back asynchronously through the
 * <code>FriendRequestDecisionHandler</code> interface. 
 */
class SubscriptionListener
implements PacketListener, PacketFilter, FriendRequestDecisionHandler {

    private static final Log LOG = LogFactory.getLog(SubscriptionListener.class);

    private final XMPPConnection connection;
    private final EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster;

    SubscriptionListener(XMPPConnection connection,
            EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster) {
        this.connection = connection;
        this.friendRequestBroadcaster = friendRequestBroadcaster;
    }

    @Override
    public void processPacket(Packet packet) {
        Presence presence = (Presence)packet;
        String friendUsername = StringUtils.parseBareAddress(packet.getFrom());
        if(presence.getType() == Type.subscribe) {
            if(LOG.isDebugEnabled())
                LOG.debug("subscribe from " + friendUsername);
            // If this is a response, handle it automatically
            Roster roster = connection.getRoster();
            if(roster != null) {
                RosterEntry entry = roster.getEntry(friendUsername);
                if(entry != null) {
                    LOG.debug("it's a response to our subscription");
                    // Acknowledge the subscription
                    Presence ack = new Presence(Presence.Type.subscribed);
                    ack.setTo(friendUsername);
                    connection.sendPacket(ack);
                    return;
                } else {
                    // This is a new friend request - ask the user
                    LOG.debug("it's a new subscription");
                    friendRequestBroadcaster.broadcast(new FriendRequestEvent(
                            new FriendRequest(friendUsername, this),
                            FriendRequest.EventType.REQUESTED));
                }
            }
        } else if(presence.getType() == Type.subscribed) {
            if(LOG.isDebugEnabled())
                LOG.debug("subscribed from " + friendUsername);
        } else if(presence.getType() == Type.unsubscribe) {
            if(LOG.isDebugEnabled())
                LOG.debug("unsubscribe from " + friendUsername);
            // If this is a response, don't respond again
            Roster roster = connection.getRoster();
            if(roster != null) {
                RosterEntry entry = roster.getEntry(friendUsername);
                if(entry == null) {
                    LOG.debug("it's a response to our unsubscription");
                    // Acknowledge the unsubscription
                    Presence ack = new Presence(Presence.Type.unsubscribed);
                    ack.setTo(friendUsername);
                    connection.sendPacket(ack);
                } else {
                    LOG.debug("it's a new unsubscription");
                    // Acknowledge the unsubscription
                    Presence ack = new Presence(Presence.Type.unsubscribed);
                    ack.setTo(friendUsername);
                    connection.sendPacket(ack);
                    // Unsubscribe from the friend
                    Presence unsub = new Presence(Presence.Type.unsubscribe);
                    unsub.setTo(friendUsername);
                    connection.sendPacket(unsub);
                    // Remove the friend from the roster
                    try {
                        roster.removeEntry(entry);
                    } catch(XMPPException x) {
                        LOG.debug(x);
                    }
                }
            }
        } else if(presence.getType() == Type.unsubscribed) {
            if(LOG.isDebugEnabled())
                LOG.debug("unsubscribed from " + friendUsername);
        }
    }

    @Override
    public boolean accept(Packet packet) {
        if(packet instanceof Presence) {
            Presence presence = (Presence)packet;
            return presence.getType() == Type.subscribe
            || presence.getType() == Type.subscribed
            || presence.getType() == Type.unsubscribe
            || presence.getType() == Type.unsubscribed;
        }
        return false;
    }

    @Override
    public void handleDecision(String friendUsername, boolean accepted) {
        if(!connection.isConnected())
            return;
        if(accepted) {
            LOG.debug("user accepted");
            // The user accepted the request - acknowledge the subscription
            Presence ack = new Presence(Presence.Type.subscribed);
            ack.setTo(friendUsername);
            connection.sendPacket(ack);
            // Subscribe to the friend
            Presence sub = new Presence(Presence.Type.subscribe);
            sub.setTo(friendUsername);
            connection.sendPacket(sub);
        } else {
            LOG.debug("user declined");
            // The user declined the request - refuse the subscription
            Presence nack = new Presence(Presence.Type.unsubscribed);
            nack.setTo(friendUsername);            
            connection.sendPacket(nack);
        }
    }
}
