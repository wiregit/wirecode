package com.limegroup.gnutella.dht;

import java.net.SocketAddress;

import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.util.HostFilter;

import com.limegroup.gnutella.RouterService;

/**
 * A Host Filter that delegates to RouterService's filter
 *
 */
public class DHTFilterDelegate implements HostFilter {

    public boolean allow(DHTMessage message) {
        SocketAddress addr = message.getContact().getContactAddress();
        return RouterService.getIpFilter().allow(addr);
    }

    public void ban(SocketAddress addr) {
        RouterService.getIpFilter().ban(addr);
        RouterService.reloadIPFilter();
    }

}
