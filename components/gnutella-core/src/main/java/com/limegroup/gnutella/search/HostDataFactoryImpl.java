package com.limegroup.gnutella.search;

import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;

@Singleton
public class HostDataFactoryImpl implements HostDataFactory {
    
    private final NetworkManager networkManager;
    private final NetworkInstanceUtils networkInstanceUtils;
    
    @Inject
    public HostDataFactoryImpl(NetworkManager networkManager, NetworkInstanceUtils networkInstanceUtils) {
        this.networkManager = networkManager;
        this.networkInstanceUtils = networkInstanceUtils;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.search.HostDataFactory#createHostData(com.limegroup.gnutella.messages.QueryReply)
     */
    public HostData createHostData(QueryReply reply) {
        byte[] clientGuid = reply.getClientGUID();
        byte[] messageGuid = reply.getGUID();
        String ip = reply.getIP();
        int port = reply.getPort();

        boolean firewalled        = true;
        boolean busy              = true;
        boolean browseHostEnabled = false;
        boolean chatEnabled       = false;
        boolean measuredSpeed     = false;
        boolean multicast         = false;
        String  vendor = "";

        try {
            firewalled = reply.getNeedsPush() || networkInstanceUtils.isPrivateAddress(ip);
        } catch (BadPacketException e) {
            firewalled = true;
        }
        
        try { 
            measuredSpeed = reply.getIsMeasuredSpeed();
        } catch (BadPacketException e) { 
            measuredSpeed = false;
        }
        try {
            busy = reply.getIsBusy();
        } catch (BadPacketException bad) {
            busy = true;
        }
        
        
        try {
            vendor = reply.getVendor();
        } catch(BadPacketException bad) {
        }

        browseHostEnabled = reply.getSupportsBrowseHost();
        chatEnabled = reply.getSupportsChat() && !firewalled;
        multicast = reply.isReplyToMulticastQuery();

        firewalled = firewalled && !multicast;
        measuredSpeed = measuredSpeed || multicast;
        boolean ifirewalled = !networkManager.acceptedIncomingConnection();
        int quality = reply.calculateQualityOfService(ifirewalled, networkManager);
        Set<? extends IpPort> proxies = reply.getPushProxies();
        boolean supportsFwt = reply.getSupportsFWTransfer();
        int fwtVersion = reply.getFWTransferVersion();
        boolean tlsCapable = reply.isTLSCapable();

        int speed;
        if ( multicast )
            speed = Integer.MAX_VALUE;
        else
            speed = ByteUtils.long2int(reply.getSpeed()); //safe cast
        
        return new HostDataImpl(clientGuid, messageGuid, speed, firewalled, busy, multicast,
                chatEnabled, browseHostEnabled, measuredSpeed, ip, port, quality, vendor, proxies,
                supportsFwt, fwtVersion, tlsCapable);
                
    }

}
