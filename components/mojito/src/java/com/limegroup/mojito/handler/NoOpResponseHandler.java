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
 
package com.limegroup.mojito.handler;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.NetworkSettings;

/**
 * The NoOpResponseHandler does litteraly nothing. Its pupose
 * is to have a non-null ResponseHandler because dealing with
 * nulls is a PITA.
 */
public final class NoOpResponseHandler implements ResponseHandler {

    private static final Log LOG = LogFactory.getLog(NoOpResponseHandler.class);
    
    private long time = 0L;
    
    private long timeout;
    
    public NoOpResponseHandler() {
        this(-1L);
    }
    
    public NoOpResponseHandler(long timeout) {
        if (timeout < 0L) {
            this.timeout = NetworkSettings.MAX_TIMEOUT.getValue();
        } else {
            this.timeout = timeout;
        }
    }
    
    public void addTime(long time) {
        this.time += time;
    }

    public void handleResponse(ResponseMessage message, long time) 
        throws IOException {
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
    }

    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        LOG.error(e);
    }
    
    public long time() {
        return time;
    }

    public long timeout() {
        return timeout;
    }
    
    public boolean isCancelled() {
        return false;
    }
}
