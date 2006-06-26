/*
 * Mojito Distributed Hash Tabe (DHT)
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
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.handler.AbstractRequestHandler;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.StatsRequest;
import com.limegroup.mojito.messages.StatsResponse;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;

/**
 * 
 */
public class StatsRequestHandler extends AbstractRequestHandler {

    private static final Log LOG = LogFactory.getLog(StatsRequestHandler.class);
    
    private final NetworkStatisticContainer networkStats;
    
    public StatsRequestHandler(Context context) {
        super(context);
        networkStats = context.getNetworkStats();
    }

    public void handleRequest(RequestMessage message) throws IOException {
        
        StatsRequest request = (StatsRequest) message;
        
        if (!request.isSecure()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(message.getContact() + " sent us an invalid Stats Request");
            }
            return;
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(message.getContact() + " sent us a Stats Request");
        }
        
        networkStats.STATS_REQUEST.incrementStat();
        StringWriter writer = new StringWriter();
        
        switch(request.getRequest()) {
            case StatsRequest.STATS:
                context.getDHTStats().dumpStats(writer, false);
                break;
            case StatsRequest.DB:
                context.getDHTStats().dumpDataBase(writer);
                break;
            case StatsRequest.RT:
                context.getDHTStats().dumpRouteTable(writer);
                break;
            default:
                if (LOG.isErrorEnabled()) {
                    LOG.error("Unknown stats request: " + request.getRequest());
                }
                return;
                
        }
        
        StatsResponse response = context.getMessageHelper()
            .createStatsResponse(message, writer.toString());
        
        context.getMessageDispatcher().send(message.getContact(), response);
    }
}
