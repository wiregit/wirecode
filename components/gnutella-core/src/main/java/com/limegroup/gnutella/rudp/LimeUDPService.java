package com.limegroup.gnutella.rudp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.limewire.io.NetworkUtils;
import org.limewire.rudp.messages.RUDPMessage;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messages.Message;

public class LimeUDPService implements org.limewire.rudp.UDPService {
    
    private final NetworkManager networkManager;
    
    public LimeUDPService(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public InetAddress getStableListeningAddress() {
        InetAddress lip = null;
        try {
            lip = InetAddress.getByName(
              NetworkUtils.ip2string(networkManager.getNonForcedAddress()));
        } catch (UnknownHostException uhe) {
            try {
                lip = InetAddress.getLocalHost();
            } catch (UnknownHostException uhe2) {
            }
        }
        return lip;

    }

    public int getStableListeningPort() {
        return ProviderHacks.getUdpService().getStableUDPPort();
    }

    public boolean isListening() {
        return ProviderHacks.getUdpService().isListening();
    }

    public boolean isNATTraversalCapable() {
        return ProviderHacks.getUdpService().canDoFWT();
    }

    public void send(RUDPMessage message, SocketAddress address) {
        ProviderHacks.getUdpService().send((Message)message, (InetSocketAddress)address);   
    }

}
