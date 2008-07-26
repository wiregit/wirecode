package org.limewire.core.impl.xmpp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.BrowseHostHandler;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.net.address.gnutella.PushProxyHolePunchAddress;
import com.limegroup.gnutella.net.address.gnutella.PushProxyMediatorAddress;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;
import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.net.address.Address;
import org.limewire.net.address.DirectConnectionAddress;
import org.limewire.rudp.RUDPUtils;

import java.net.InetSocketAddress;
import java.util.Set;

@Singleton
public class BrowseHost {

    private final PushEndpointFactory pushEndpointFactory;
    private final SearchServices searchServices;

    @Inject
    public BrowseHost(PushEndpointFactory pushEndpointFactory, SearchServices searchServices) {
        this.pushEndpointFactory = pushEndpointFactory;
        this.searchServices = searchServices;
    }

    void browseHost(Address addr) {
        byte [] guidBytes = null;
        Set<? extends IpPort> proxies = null;
        byte features = ~PushEndpoint.PPTLS_BINARY;
        int version = 0;
        IpPort directAddrss = null;

        if(addr instanceof DirectConnectionAddress) {
            DirectConnectionAddress address = (DirectConnectionAddress)addr;
            directAddrss = address;
        }
        else if(addr instanceof PushProxyMediatorAddress) {
            PushProxyMediatorAddress address = (PushProxyMediatorAddress)addr;
            guidBytes = address.getClientID().bytes();
            proxies = address.getPushProxies();
        } else if(addr instanceof PushProxyHolePunchAddress) {
            PushProxyHolePunchAddress address = (PushProxyHolePunchAddress) addr;
            guidBytes = ((PushProxyMediatorAddress)address.getMediatorAddress()).getClientID().bytes();
            proxies = ((PushProxyMediatorAddress)address.getMediatorAddress()).getPushProxies();
            version = address.getVersion();
            BitNumbers bn = HTTPHeaderUtils.getTLSIndices(proxies, (Math.min(proxies.size(), PushEndpoint.MAX_PROXIES)));
            features = bn.toByteArray()[0] |= PushEndpoint.PPTLS_BINARY;
            directAddrss = address.getDirectConnectionAddress();
        }

        PushEndpoint pushEndpoint = pushEndpointFactory.createPushEndpoint(guidBytes, proxies, features, version, directAddrss);
        InetSocketAddress inetSocketAddress = pushEndpoint.getInetSocketAddress();
        Connectable host = inetSocketAddress != null ? new ConnectableImpl(inetSocketAddress, false) : null;


        GUID guid = new GUID(GUID.makeGuid());

        BrowseHostHandler bhh = searchServices.doAsynchronousBrowseHost(
                                    host, guid, new GUID(pushEndpoint.getClientGUID()), pushEndpoint.getProxies(),
                                    pushEndpoint.getFWTVersion() >= RUDPUtils.VERSION);
    }
}
