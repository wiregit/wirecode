package org.limewire.xmpp.client.impl.messages.discoinfo;

import java.net.URI;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.core.api.friend.client.FriendConnection;

/**
 * sends disco info messages (http://jabber.org/protocol/disco#info) to newly available
 * presences and then calls the appropriate FeatureInitializer for each of the 
 * features that come back in the response.
 */
public class DiscoInfoListener implements PacketListener {
    
    private static final Log LOG = LogFactory.getLog(DiscoInfoListener.class);
    
    private final FriendConnection connection;
    private final org.jivesoftware.smack.XMPPConnection smackConnection;
    private final FeatureRegistry featureRegistry;
    private final RosterListener rosterListener;
    private final PresenceListener presenceListener;      

    public DiscoInfoListener(FriendConnection connection,
                             org.jivesoftware.smack.XMPPConnection smackConnection,
                             FeatureRegistry featureRegistry) {
        this.connection = connection;
        this.smackConnection = smackConnection;
        this.featureRegistry = featureRegistry;
        rosterListener = new RosterListener();
        presenceListener = new PresenceListener();
    }

    @Override
    public void processPacket(Packet packet) {
        DiscoverInfo discoverInfo = (DiscoverInfo)packet;
        Friend user = connection.getUser(StringUtils.parseBareAddress(discoverInfo.getFrom()));
        if (user != null) {
            FriendPresence presence = user.getFriendPresences().get(discoverInfo.getFrom());
            if(presence != null) {
                for(URI uri : featureRegistry) {
                    if(discoverInfo.containsFeature(uri.toASCIIString())) {
                        LOG.debugf("initializing feature {0} for {1}", uri.toASCIIString(), presence.getPresenceId());
                        featureRegistry.get(uri).initializeFeature(presence);
                    }
                }
            }
        }
    }
    
    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof DiscoverInfo && 
                        (((DiscoverInfo)packet).getType() == IQ.Type.SET
                        || ((DiscoverInfo)packet).getType() == IQ.Type.RESULT);
            }
        };
    }
    
    public RosterListener getRosterListener() {
        return rosterListener;
    }
    
    class RosterListener implements EventListener<RosterEvent> {
        @Override
        public void handleEvent(RosterEvent event) {
            if(event.getType() == RosterEvent.Type.USER_ADDED) {
                event.getData().addPresenceListener(presenceListener);
            }
        }
    }

    private class PresenceListener implements EventListener<PresenceEvent> {
        @Override
        public void handleEvent(final PresenceEvent event) {
            if(event.getType() == PresenceEvent.Type.PRESENCE_NEW
                    && event.getData().getType() == FriendPresence.Type.available) {
                Thread t = ThreadExecutor.newManagedThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(smackConnection);
                            serviceDiscoveryManager.discoverInfo(event.getData().getPresenceId());
                        } catch (org.jivesoftware.smack.XMPPException exception) {
                            if(exception.getXMPPError() != null && exception.getXMPPError().getCode() != 501) {
                                LOG.info(exception.getMessage(), exception);
                            }
                        }
                    }
                }, "disco-info-" + event.getData().getPresenceId());
                t.start();
            }
        }
    }
}
