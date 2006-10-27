package com.limegroup.gnutella.dht;

import java.net.SocketAddress;

import com.limegroup.gnutella.RouterService;
import com.limegroup.mojito.util.HostFilter;

/**
 * A Host Filter that delegates to RouterService's filter
 *
 */
public class DHTFilterDelegate implements HostFilter {

    public boolean allow(SocketAddress addr) {
        return RouterService.getIpFilter().allow(addr);
    }

    public void ban(SocketAddress addr) {
        RouterService.getIpFilter().ban(addr);
        RouterService.reloadIPFilter();
    }

}
