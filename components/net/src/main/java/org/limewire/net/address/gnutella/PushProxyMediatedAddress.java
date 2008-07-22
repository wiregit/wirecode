package org.limewire.net.address.gnutella;

import java.util.List;

import org.limewire.net.address.MediatedConnectionAddress;

import com.limegroup.gnutella.GUID;

public interface PushProxyMediatedAddress extends MediatedConnectionAddress {
    public GUID getClientID();
    public List<PushProxyAddress> getPushProxies();
}
