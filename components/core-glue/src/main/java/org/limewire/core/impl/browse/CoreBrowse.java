package org.limewire.core.impl.browse;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.collection.BitNumbers;
import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.impl.search.QueryReplyListener;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.net.address.Address;
import org.limewire.net.address.DirectConnectionAddress;
import org.limewire.rudp.RUDPUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.net.address.gnutella.PushProxyHolePunchAddress;
import com.limegroup.gnutella.net.address.gnutella.PushProxyMediatorAddress;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;

public class CoreBrowse implements Browse {    
    private final SearchServices searchServices;
    private final PushEndpointFactory pushEndpointFactory;
    
    private final Address addr;
    private final QueryReplyListenerList listenerList;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile byte[] browseGuid;
    private volatile QueryReplyListener listener;

    @AssistedInject
    public CoreBrowse(@Assisted
    Address address, SearchServices searchServices,
                     QueryReplyListenerList listenerList, PushEndpointFactory pushEndpointFactory) {
        this.addr = address;
        this.searchServices = searchServices;
        this.listenerList = listenerList;
        this.pushEndpointFactory = pushEndpointFactory;
    }

    @Override
    public void start(final BrowseListener browseListener) {
        if (started.getAndSet(true)) {
            throw new IllegalStateException("already started!");
        }

        browseGuid = searchServices.newQueryGUID();
        listener = new BrowseResultAdapter(browseListener);
        listenerList.addQueryReplyListener(browseGuid, listener);
        
        Connectable host = null;
        byte [] clientGuid = null;
        Set<? extends IpPort> proxies = null;
        boolean canDoFWT = false;

        if(addr instanceof DirectConnectionAddress) {
            DirectConnectionAddress address = (DirectConnectionAddress)addr;
            host = address;
        }
        else if(addr instanceof PushProxyMediatorAddress) {
            PushProxyMediatorAddress address = (PushProxyMediatorAddress)addr;
            clientGuid = address.getClientID().bytes();
            proxies = address.getPushProxies();
        } else if(addr instanceof PushProxyHolePunchAddress) {
            PushProxyHolePunchAddress address = (PushProxyHolePunchAddress) addr;
            clientGuid = ((PushProxyMediatorAddress)address.getMediatorAddress()).getClientID().bytes();
            proxies = ((PushProxyMediatorAddress)address.getMediatorAddress()).getPushProxies();
            int version = address.getVersion();
            BitNumbers bn = HTTPHeaderUtils.getTLSIndices(proxies, (Math.min(proxies.size(), PushEndpoint.MAX_PROXIES)));
            // TODO fix case with empty byte []
            byte features = bn.toByteArray()[0] |= PushEndpoint.PPTLS_BINARY;
            IpPort directAddrss = address.getDirectConnectionAddress();
            PushEndpoint pushEndpoint = pushEndpointFactory.createPushEndpoint(clientGuid, proxies, features, version, directAddrss);
            proxies = pushEndpoint.getProxies();
            canDoFWT = pushEndpoint.getFWTVersion() >= RUDPUtils.VERSION;
            InetSocketAddress inetSocketAddress = pushEndpoint.getInetSocketAddress();
            host = inetSocketAddress != null ? new ConnectableImpl(inetSocketAddress, false) : null;
        }

        searchServices.doAsynchronousBrowseHost(
                                    host, new GUID(browseGuid), clientGuid != null ? new GUID(clientGuid) : null, proxies,
                                    canDoFWT);
    }

    @Override
    public void stop() {
        listenerList.removeQueryReplyListener(browseGuid, listener);
        searchServices.stopQuery(new GUID(browseGuid));
    }

    private static class BrowseResultAdapter implements QueryReplyListener {
        private final BrowseListener browseListener;

        public BrowseResultAdapter(BrowseListener browseListener) {
            this.browseListener = browseListener;
        }

        @Override
        public void handleQueryReply(RemoteFileDesc rfd, QueryReply queryReply,
                Set<? extends IpPort> locs) {
            browseListener.handleBrowseResult(new RemoteFileDescAdapter(rfd,
                    queryReply, locs));
        }
    }

    public GUID getQueryGuid() {
        return new GUID(browseGuid);
    }
}
