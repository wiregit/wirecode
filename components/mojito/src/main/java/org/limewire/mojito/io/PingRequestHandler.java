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
 
package org.limewire.mojito.io;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.message.MessageHelper;
import org.limewire.mojito.message.PingRequest;
import org.limewire.mojito.message.PingResponse;
import org.limewire.mojito.message.RequestMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.MessageUtils;


/**
 * Handles incoming Ping requests.
 */
public class PingRequestHandler extends AbstractRequestHandler {
    
    private static final Log LOG 
        = LogFactory.getLog(PingRequestHandler.class);
    
    public PingRequestHandler(Context context) {
        super(context);
    }
    
    /**
     * Returns true if the "localhost" is in the process of booting
     * and the PING is not a collision 
     */
    private boolean isBooting(PingRequest message) {
        Contact localhost = context.getLocalhost();
        KUID contactId = localhost.getContactId();
        
        boolean collisionPing = MessageUtils.isCollisionPingRequest(contactId, message);
        return context.isBooting() && !collisionPing;
    }
    
    @Override
    protected void processRequest(RequestMessage message) throws IOException {
        
        PingRequest request = (PingRequest)message;
        Contact contact = request.getContact();
        
        // Don't respond to PINGs while we're bootstrapping! This makes
        // sure nobody can use as their initial bootstrap Node as we've
        // (likely) poor knowledge of the DHT and it's best to ignore
        // the request. The only exception are collision test PINGs.
        if (isBooting(request)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Ignoring PING from " + contact);
            }
            return;
        }
        
        MessageHelper messageHelper = context.getMessageHelper();
        PingResponse response = messageHelper.createPingResponse(
                request, contact.getContactAddress());

        send(contact, response);
    }
}
