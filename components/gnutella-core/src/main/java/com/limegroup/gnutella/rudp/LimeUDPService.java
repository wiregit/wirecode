package com.limegroup.gnutella.rudp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.limewire.io.NetworkUtils;
import org.limewire.rudp.messages.RUDPMessage;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.Message;

public class LimeUDPService implements org.limewire.rudp.UDPService {

    public InetAddress getStableListeningAddress() {
        InetAddress lip = null;
        try {
            lip = InetAddress.getByName(
              NetworkUtils.ip2string(RouterService.getNonForcedAddress()));
        } catch (UnknownHostException uhe) {
            try {
                lip = InetAddress.getLocalHost();
            } catch (UnknownHostException uhe2) {
            }
        }
        return lip;

    }

    public int getStableListeningPort() {
        return UDPService.instance().getStableUDPPort();
    }

    public boolean isListening() {
        return UDPService.instance().isListening();
    }

    public boolean isNATTraversalCapable() {
        return UDPService.instance().canDoFWT();
    }

    public void send(RUDPMessage message, SocketAddress address) {
        UDPService.instance().send((Message)message, (InetSocketAddress)address);   
    }

}
