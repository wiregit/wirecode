/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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
 
package org.limewire.mojito.handler.request;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context2;
import org.limewire.mojito.message2.MessageHelper2;
import org.limewire.mojito.message2.PingRequest;
import org.limewire.mojito.message2.PingResponse;
import org.limewire.mojito.message2.RequestMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.MessageUtils;


/**
 * Handles incoming Ping requests.
 */
public class PingRequestHandler2 extends AbstractRequestHandler2 {
    
    private static final Log LOG 
        = LogFactory.getLog(PingRequestHandler2.class);
    
    public PingRequestHandler2(Context2 context) {
        super(context);
    }
    
    @Override
    protected void processRequest(RequestMessage message) throws IOException {
        
        PingRequest request = (PingRequest)message;
        Contact node = request.getContact();
        
        // Don't respond to pings while we're bootstrapping! This
        // makes sure nobody can use us as the initial bootstrap
        // Node as we've (likely) poor knowledge of the DHT in this
        // stage. The only exception from this are collision test
        // pings!
        if (context.isBootstrapping() 
                && !MessageUtils.isCollisionPingRequest(
                        context.getLocalNodeID(), message)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Received a PingRequest from " + node 
                        + " but local Node is bootstrapping");
            }
            return;
        }
        
        MessageHelper2 messageHelper = context.getMessageHelper();
        PingResponse response = messageHelper.createPingResponse(
                request, node.getContactAddress());

        send(node, response);
    }
}
