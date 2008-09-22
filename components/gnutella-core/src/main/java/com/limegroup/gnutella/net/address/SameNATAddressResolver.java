package com.limegroup.gnutella.net.address;

import java.util.Arrays;

import org.limewire.io.Address;
import org.limewire.io.NetworkUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.net.address.AddressResolver;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;

/**
 * Detects if a firewalled address is behind the same NAT and on the same
 * local network and resolves to the local address. Otherwise marks the 
 * firewalled address as resolved.
 */
@Singleton
public class SameNATAddressResolver implements AddressResolver {

    private final static Log LOG = LogFactory.getLog(SameNATAddressResolver.class);
    
    private final NetworkManager networkManager;

    @Inject
    public SameNATAddressResolver(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }
    
    @Inject
    public void register(SocketsManager socketsManager) {
        socketsManager.registerResolver(this);
    }
    
    @Override
    public boolean canResolve(Address address) {
        return address instanceof FirewalledAddress && !(address instanceof ResolvedFirewalledAddress); 
    }

    @Override
    public void resolve(Address addr, int timeout, AddressResolutionObserver observer) {
        FirewalledAddress address = (FirewalledAddress)addr;
        byte[] publicAddress = networkManager.getExternalAddress();
        if (!Arrays.equals(publicAddress, address.getPublicAddress().getInetAddress().getAddress())) {
            LOG.debugf("different public address: {0}", address.getPublicAddress());
            observer.resolved(new ResolvedFirewalledAddress(address.getPublicAddress(), address.getPrivateAddress(), address.getClientGuid(), address.getPushProxies(), address.getFwtVersion()));
            return;
        }
        byte[] privateAddress = networkManager.getAddress();
        if (NetworkUtils.areInSameSiteLocalNetwork(privateAddress, address.getPrivateAddress().getInetAddress().getAddress())) {
            LOG.debug("addresses behind same NAT!");
            observer.resolved(address.getPrivateAddress());
        } else {
            LOG.debugf("different site local networks: {0}", address.getPrivateAddress());
            observer.resolved(new ResolvedFirewalledAddress(address.getPublicAddress(), address.getPrivateAddress(), address.getClientGuid(), address.getPushProxies(), address.getFwtVersion()));
        }
    }

}
