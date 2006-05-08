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

package de.kapsi.net.kademlia.handler;

import java.io.IOException;
import java.net.SocketAddress;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.settings.NetworkSettings;

public final class NoOpResponseHandler implements ResponseHandler {

    private long time = 0L;
    
    private long timeout;
    
    public NoOpResponseHandler() {
        this(-1L);
    }
    
    public NoOpResponseHandler(long timeout) {
        if (timeout < 0L) {
            this.timeout = NetworkSettings.TIMEOUT.getValue();
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

    public long time() {
        return time;
    }

    public long timeout() {
        return timeout;
    }
}
