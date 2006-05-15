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

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.PingListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.response.PingResponse;

public class PingResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(PingResponseHandler.class);
    
    public PingResponseHandler(Context context) {
        super(context);
    }

    public void addPingListener(PingListener listener) {
        listeners.add(listener);
    }
    
    public void removePingListener(PingListener listener) {
        listeners.remove(listener);
    }

    public PingListener[] getPingListeners() {
        return (PingListener[])listeners.toArray(new PingListener[0]);
    }

    protected void response(ResponseMessage message, long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Received pong from " + message.getSource() 
                    + " after " + getErrors() + " errors and a total time of " + time() + "ms");
        }
        
        PingResponse response = (PingResponse)message;
        SocketAddress externalAddress = response.getExternalSocketAddress();
        
        if (message.getSourceAddress().equals(externalAddress)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(message.getSource() + " is trying to set our external address to its address!");
            }
            return;
        }
        
        context.setExternalSocketAddress(externalAddress);
        context.addEstimatedRemoteSize(response.getEstimatedSize());
    }

    public void handleTimeout(KUID nodeId, 
            SocketAddress dst, RequestMessage message, long time) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Ping to " + ContactNode.toString(nodeId, dst) 
                    + " failed after " + time + "ms");
        }

        super.handleTimeout(nodeId, dst, message, time);
    }
    
    protected void resend(KUID nodeId, SocketAddress dst, RequestMessage message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Re-sending Ping to " + ContactNode.toString(nodeId, dst));
        }
        
        super.resend(nodeId, dst, message);
    }

    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Giving up to ping " + ContactNode.toString(nodeId, dst) 
                    + " after " + getMaxErrors() + " errors and a total time of "+ time() + "ms");
        }
    }
    
    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        if (LOG.isErrorEnabled()) {
            LOG.error("Sending a ping to " + ContactNode.toString(nodeId, dst) + " failed", e);
        }
        
        fireTimeout(nodeId, dst, message, -1L);
    }
}
