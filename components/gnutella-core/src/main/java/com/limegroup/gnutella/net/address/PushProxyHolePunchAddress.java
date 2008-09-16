package com.limegroup.gnutella.net.address;

import org.limewire.net.address.HolePunchAddress;

/**
 * Represent the information necessary to execute a firewall-to-firewall
 * transfer, using push-proxies as the signaling channel.
 */
public interface PushProxyHolePunchAddress extends HolePunchAddress {
    
    @Override
    public PushProxyMediatorAddress getMediatorAddress();
}
