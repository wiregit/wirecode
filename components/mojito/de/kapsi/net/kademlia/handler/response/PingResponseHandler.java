/*
 * Lime Kademlia Distributed Hash Table (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.NetworkStatisticContainer;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.handler.request.PingRequestHandler;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.response.PingResponse;

public class PingResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(PingRequestHandler.class);
    
    private PingListener l;
    
    private final NetworkStatisticContainer networkStats;
    
    public PingResponseHandler(Context context, PingListener l) {
        super(context);
        networkStats = context.getNetworkStats();
        this.l = l;
    }

    public void handleResponse(final ResponseMessage message, final long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Ping to " + message.getContactNode() + " succeeded");
        }
        
        networkStats.PINGS_OK.incrementStat();
        
        PingResponse response = (PingResponse)message;
        
        context.setExternalSocketAddress(response.getSocketAddress());
        context.addEstimatedRemoteSize(response.getEstimatedSize());
        
        if (l != null) {
            context.fireEvent(new Runnable() {
                public void run() {
                    l.pingSuccess(message.getContactNode(), time);
                }
            });
        }
    }
    
    public void handleTimeout(final KUID nodeId, final SocketAddress dst, long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Ping to " + ContactNode.toString(nodeId, dst) + " failed");
        }
        
        networkStats.PINGS_FAILED.incrementStat();
        
        if (l != null) {
            context.fireEvent(new Runnable() {
                public void run() {
                    l.pingTimeout(nodeId, dst);
                }
            });
        }
    }
}
