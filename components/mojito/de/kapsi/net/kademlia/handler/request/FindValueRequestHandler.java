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
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.request.FindValueRequest;
import de.kapsi.net.kademlia.messages.request.LookupRequest;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;

public class FindValueRequestHandler extends LookupRequestHandler {
    
    private static final Log LOG = LogFactory.getLog(FindValueRequestHandler.class);
    
    public FindValueRequestHandler(Context context) {
        super(context);
    }

    public void handleRequest(RequestMessage message) throws IOException {
        
        if (message instanceof FindValueRequest) {
            LookupRequest request = (LookupRequest)message;
            KUID lookup = request.getLookupID();

            Collection values = context.getDatabase().get(lookup);
            if (values != null && !values.isEmpty()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Hit! " + lookup + " = " + values);
                }
                
                FindValueResponse response = context.getMessageFactory()
                            .createFindValueResponse(request, values);
                context.getMessageDispatcher().send(message.getContactNode(), response, null);
                
                return; // We're done here!
            }
        }
        
        super.handleRequest(message);
    }
}
