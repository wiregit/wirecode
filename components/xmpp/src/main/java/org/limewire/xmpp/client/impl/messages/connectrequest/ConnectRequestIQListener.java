package org.limewire.xmpp.client.impl.messages.connectrequest;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.listener.EventBroadcaster;
import org.limewire.net.ConnectRequestEvent;

public class ConnectRequestIQListener implements PacketListener {

    private final EventBroadcaster<ConnectRequestEvent> connectRequestEventBroadcaster;

    public ConnectRequestIQListener(EventBroadcaster<ConnectRequestEvent> connectRequestEventBroadcaster) {
        this.connectRequestEventBroadcaster = connectRequestEventBroadcaster;
    }
    
    @Override
    public void processPacket(Packet packet) {
        ConnectRequestIQ connectRequest = (ConnectRequestIQ)packet;
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
