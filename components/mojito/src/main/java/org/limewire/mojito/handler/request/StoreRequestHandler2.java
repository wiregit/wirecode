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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.mojito.Context2;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.io.MessageDispatcher2;
import org.limewire.mojito.messages.MessageHelper2;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.StoreRequest;
import org.limewire.mojito.messages.StoreResponse;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;
import org.limewire.security.SecurityToken.TokenData;


/**
 * Handles incoming store requests as 
 * sent by other Nodes. It performs some probability tests to
 * make sure the request makes sense (i.e. if the Key is close
 * to us and so on).
 */
public class StoreRequestHandler2 extends AbstractRequestHandler2 {
    
    private static final Log LOG 
        = LogFactory.getLog(StoreRequestHandler.class);
    
    @InspectablePrimitive(value = "No Security Token")
    private static final AtomicInteger NO_SECURITY_TOKEN = new AtomicInteger();
    
    @InspectablePrimitive(value = "Bad Security Token")
    private static final AtomicInteger BAD_SECURITY_TOKEN = new AtomicInteger();
    
    @InspectablePrimitive(value = "Store Success Count")
    private static final AtomicInteger STORE_SUCCESS = new AtomicInteger();
    
    @InspectablePrimitive(value = "Store Failure Count")
    private static final AtomicInteger STORE_FAILURE = new AtomicInteger();
    
    public StoreRequestHandler2(MessageDispatcher2 messageDispatcher, Context2 context) {
        super(messageDispatcher, context);
    }
    
    @Override
    protected void processRequest(RequestMessage message) throws IOException {
        
        StoreRequest request = (StoreRequest)message;
        SecurityToken securityToken = request.getSecurityToken();
        
        if (securityToken == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error(request.getContact() 
                        + " does not provide a SecurityToken");
            }
            NO_SECURITY_TOKEN.incrementAndGet();
            return;
        }
        
        Contact src = request.getContact();
        TokenData expectedToken = context.getSecurityTokenHelper().createTokenData(src);
        if (!securityToken.isFor(expectedToken)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(request.getContact() 
                        + " send us an invalid SecurityToken " + securityToken);
            }
            BAD_SECURITY_TOKEN.incrementAndGet();
            return;
        }
        
        Collection<? extends DHTValueEntity> values = request.getDHTValueEntities();
        
        List<StoreStatusCode> status 
            = new ArrayList<StoreStatusCode>(values.size());
        
        Database database = context.getDatabase();
        
        for (DHTValueEntity entity : values) {
            
            if (database.store(entity)) {
                STORE_SUCCESS.incrementAndGet();
                status.add(new StoreStatusCode(entity, StoreResponse.OK));
            } else {
                STORE_FAILURE.incrementAndGet();
                status.add(new StoreStatusCode(entity, StoreResponse.ERROR));
            }
        }
        
        MessageHelper2 messageHelper = context.getMessageHelper();
        StoreResponse response = messageHelper.createStoreResponse(request, status);
        messageDispatcher.send(request.getContact(), response);
    }
}
