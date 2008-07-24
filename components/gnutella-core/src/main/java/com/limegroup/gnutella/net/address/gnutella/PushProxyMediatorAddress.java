package com.limegroup.gnutella.net.address.gnutella;

import java.util.Set;

import org.limewire.net.address.MediatorAddress;

import com.limegroup.gnutella.GUID;

/**
 * Represent the information necessary to execute a push (i.e., reverse connect),
 * using push-proxies as the signaling channel.
 */
public interface PushProxyMediatorAddress extends MediatorAddress {
    public GUID getClientID();
    public Set<PushProxyAddress> getPushProxies();
}
