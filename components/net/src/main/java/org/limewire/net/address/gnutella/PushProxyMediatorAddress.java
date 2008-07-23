package org.limewire.net.address.gnutella;

import java.util.List;

import org.limewire.net.address.MediatorAddress;

import com.limegroup.gnutella.GUID;

public interface PushProxyMediatorAddress extends MediatorAddress {
    public GUID getClientID();
    public List<PushProxyAddress> getPushProxies();
}
