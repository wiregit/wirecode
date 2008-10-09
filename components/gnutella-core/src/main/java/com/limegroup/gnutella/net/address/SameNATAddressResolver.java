package com.limegroup.gnutella.net.address;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.NetworkUtils;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressEvent;
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
public class SameNATAddressResolver implements AddressResolver, RegisteringEventListener<AddressEvent> {

    private final static Log LOG = LogFactory.getLog(SameNATAddressResolver.class);
    
    private final NetworkManager networkManager;
    
    private final AtomicReference<Address> localAddress = new AtomicReference<Address>();

    @Inject
    public SameNATAddressResolver(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }
    
    @Inject
    public void register(SocketsManager socketsManager) {
        socketsManager.registerResolver(this);
    }

    @Inject
    public void register(ListenerSupport<AddressEvent> addressEventListenerSupport) {
        addressEventListenerSupport.addListener(this);
    }

    public void handleEvent(AddressEvent event) {
        synchronized (localAddress) {
            localAddress.set(event.getSource());
            localAddress.notifyAll();
        }
    }

    @Override
    public boolean canResolve(Address address) {
        return address instanceof FirewalledAddress && !(address instanceof ResolvedFirewalledAddress); 
    }

    /**
     * Resolves a {@link FirewalledAddress} to the {@link Connectable} of its 
     * {@link FirewalledAddress#getPrivateAddress() private address} if this peer
     * and the peer the address belongs to are behind the same firewall.
     * 
     * Otherwise resolves the address to a {@link ResolvedFirewalledAddress} to
     * mark it as resolved.
     */
    @Override
    public void resolve(Address addr, int timeout, AddressResolutionObserver observer) {
        FirewalledAddress address = (FirewalledAddress)addr;
        waitForLocalAddress();
        byte[] publicAddress = networkManager.getExternalAddress();
        if (!Arrays.equals(publicAddress, address.getPublicAddress().getInetAddress().getAddress())) {
            LOG.debugf("different public address: local = {0}, remote = {1}", toString(publicAddress), address.getPublicAddress());
            observer.resolved(new ResolvedFirewalledAddress(address.getPublicAddress(), address.getPrivateAddress(), address.getClientGuid(), address.getPushProxies(), address.getFwtVersion()));
            return;
        }
        byte[] privateAddress = networkManager.getNonForcedAddress();
        if (NetworkUtils.areInSameSiteLocalNetwork(privateAddress, address.getPrivateAddress().getInetAddress().getAddress())) {
            LOG.debug("addresses behind same NAT!");
            observer.resolved(address.getPrivateAddress());
        } else {
            LOG.debugf("different site local networks: local = {0}, remote = {1}", toString(privateAddress), address.getPrivateAddress());
            observer.resolved(new ResolvedFirewalledAddress(address.getPublicAddress(), address.getPrivateAddress(), address.getClientGuid(), address.getPushProxies(), address.getFwtVersion()));
        }
    }

    private String toString(byte[] publicAddress) {
        StringBuilder sb = new StringBuilder();
        for(byte b : publicAddress) {
            sb.append(b).append(".");
        }
        return sb.substring(0, sb.length() - 1);
    }

    private void waitForLocalAddress() {
        synchronized (localAddress) {
            while(localAddress.get() == null) {
                try {
                    localAddress.wait();
                } catch (InterruptedException e) {
                    LOG.error(e);
                }
            }
        }
    }

}
