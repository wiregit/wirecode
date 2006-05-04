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
import de.kapsi.net.kademlia.event.StatsListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.response.StatsResponse;

public class StatsResponseHandler extends AbstractResponseHandler {

    private static final Log LOG = LogFactory.getLog(StatsResponseHandler.class);
    
    private StatsListener l;
    
    public StatsResponseHandler(Context context, StatsListener l) {
        super(context);
        this.l = l;
    }

    
    protected void response(final ResponseMessage message, final long time) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Stats request to " + message.getContactNode() + " succeeded");
        }
        
        final StatsResponse response = (StatsResponse) message;
        
        if (l != null) {
            context.fireEvent(new Runnable() {
                public void run() {
                    l.nodeStatsResponse(response.getContactNode(), response.getStatistics(), time);
                }
            });
        }
    }


    protected void timeout(final KUID nodeId, final SocketAddress dst, 
            final RequestMessage message, final long time) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Stats request to " + ContactNode.toString(nodeId, dst) 
                    + " failed");
        }
        
        if (l != null) {
            context.fireEvent(new Runnable() {
                public void run() {
                    l.nodeStatsTimeout(nodeId, dst);
                }
            });
        }
    }
}
