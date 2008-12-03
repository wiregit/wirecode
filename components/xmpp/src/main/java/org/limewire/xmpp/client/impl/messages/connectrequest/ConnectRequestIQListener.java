package org.limewire.xmpp.client.impl.messages.connectrequest;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectRequestEvent;

/**
 * Listens for {@link ConnectRequestIQ connect request iqs} and fires
 * a {@link ConnectRequestEvent}.
 */
public class ConnectRequestIQListener implements PacketListener {

    private static final Log LOG = LogFactory.getLog(ConnectRequestIQListener.class);
    
    private final EventBroadcaster<ConnectRequestEvent> connectRequestEventBroadcaster;

    public ConnectRequestIQListener(EventBroadcaster<ConnectRequestEvent> connectRequestEventBroadcaster) {
        this.connectRequestEventBroadcaster = connectRequestEventBroadcaster;
    }
    
    @Override
    public void processPacket(Packet packet) {
        ConnectRequestIQ connectRequest = (ConnectRequestIQ)packet;
        LOG.debugf("processing connect request: {0}", connectRequest);
        connectRequestEventBroadcaster.broadcast(new ConnectRequestEvent(connectRequest.getAddress(), connectRequest.getClientGuid(), connectRequest.getSupportedFWTVersion()));
    }
    
    public PacketFilter getPacketFilter() {
        return new PacketFilter() {
            @Override
            public boolean accept(Packet packet) {
                return packet instanceof ConnectRequestIQ;
            }
        };
    }

}
