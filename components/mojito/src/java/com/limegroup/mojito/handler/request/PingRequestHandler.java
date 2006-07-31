/*
 * Mojito Distributed Hash Table (Mojito DHT)
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
 
package com.limegroup.mojito.handler.request;

import java.io.IOException;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.handler.AbstractRequestHandler;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;

/**
 * The PingRequestHandler handles incoming Ping requests.
 */
public class PingRequestHandler extends AbstractRequestHandler {
    
    private final NetworkStatisticContainer networkStats;
    
    public PingRequestHandler(Context context) {
        super(context);
        networkStats = context.getNetworkStats();
    }
    
    @Override
    public void request(RequestMessage message) throws IOException {
        
        networkStats.PING_REQUESTS.incrementStat();
        
        PingRequest request = (PingRequest)message;
        
        Contact node = request.getContact();
        PingResponse response = context.getMessageHelper()
                .createPingResponse(request, node.getContactAddress());

        context.getMessageDispatcher().send(node, response);
        networkStats.PONGS_SENT.incrementStat();
    }
}
