package com.limegroup.gnutella.net.address.gnutella;

import java.util.List;
import java.util.Set;

import org.limewire.net.address.MediatorAddress;

import com.limegroup.gnutella.GUID;

public interface PushProxyMediatorAddress extends MediatorAddress {
    public GUID getClientID();
    public Set<PushProxyAddress> getPushProxies();
}
