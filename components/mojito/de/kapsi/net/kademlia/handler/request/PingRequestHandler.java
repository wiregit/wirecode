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
 
package de.kapsi.net.kademlia.handler.request;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.NetworkStatisticContainer;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.handler.AbstractRequestHandler;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.messages.response.PingResponse;

/**
 * 
 * @author Roger Kapsi
 */
public class PingRequestHandler extends AbstractRequestHandler {

    private static final Log LOG = LogFactory.getLog(PingRequestHandler.class);
    
    private final NetworkStatisticContainer networkStats;
    
    public PingRequestHandler(Context context) {
        super(context);
        networkStats = context.getNetworkStats();
    }
    
    public void handleRequest(RequestMessage message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(message.getContactNode() + " sent us a Ping");
        }
        
        networkStats.PING_REQUESTS.incrementStat();
        
        PingRequest request = (PingRequest)message;
        
        PingResponse response = context.getMessageFactory()
                .createPingResponse(message, message.getSocketAddress());

        context.getMessageDispatcher().send(message.getContactNode(), response, null);
        networkStats.PONGS_SENT.incrementStat();
    }
}
