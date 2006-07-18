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
 
package com.limegroup.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.StatsResponse;

/**
 * 
 */
public class StatsResponseHandler extends AbstractResponseHandler<StatsResponse> {

    private static final Log LOG = LogFactory.getLog(StatsResponseHandler.class);
    
    public StatsResponseHandler(Context context) {
        super(context);
    }
    
    protected void response(ResponseMessage message, long time) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Stats request to " + message.getContact() + " succeeded");
        }
        
        setReturnValue((StatsResponse)message);
    }

    protected void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        fireTimeoutException(nodeId, dst, message, time);
    }
    
    public void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        setException(new Exception(message.toString(), e));
    }
}
