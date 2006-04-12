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
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.NetworkStatisticContainer;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.AbstractRequestHandler;
import de.kapsi.net.kademlia.messages.Message;
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
    
    public void handleRequest(KUID nodeId, SocketAddress src, Message message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(ContactNode.toString(nodeId, src) + " sent us a Ping");
        }
        
        networkStats.PING_REQUESTS.incrementStat();
        
        PingRequest request = (PingRequest)message;
        PingResponse response = null;
        
        if (request.isSignatureRequest()) {
            try {
                response = context.getMessageFactory()
                    .createSignedPingResponse(message.getMessageID(), src, 
                            context.getPrivateKey());
                networkStats.SIGNED_PONGS_SENT.incrementStat();
            } catch (InvalidKeyException e) {
                LOG.error(e);
            } catch (SignatureException e) {
                LOG.error(e);
            }
        } else {
            response = context.getMessageFactory()
                .createPingResponse(message.getMessageID(), src);
            networkStats.PONGS_SENT.incrementStat();
        }

        if (response != null) {
            context.getMessageDispatcher().send(src, response, null);
        }
    }
}
