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

package org.limewire.mojito.entity;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.mojito.KUID;
import org.limewire.mojito.io.RequestHandle;

/**
 * 
 */
public class RequestTimeoutException extends TimeoutException {
    
    private static final long serialVersionUID = -6706120265698443279L;

    private final RequestHandle handle;
    
    private final long time;
    
    private final TimeUnit unit;
    
    public RequestTimeoutException(KUID contactId, SocketAddress address, 
            long time, TimeUnit unit) {
        
        this.handle = new RequestHandle(contactId, address, null);
        this.time = time;
        this.unit = unit;
    }
    
    public RequestTimeoutException(RequestHandle handle, 
            long time, TimeUnit unit) {
        this.handle = handle;
        this.time = time;
        this.unit = unit;
    }
    
    public RequestHandle getHandle() {
        return handle;
    }
    
    public long getTime(TimeUnit unit) {
        return unit.convert(time, this.unit);
    }
    
    public long getTimeInMillis() {
        return getTime(TimeUnit.MILLISECONDS);
    }
}
