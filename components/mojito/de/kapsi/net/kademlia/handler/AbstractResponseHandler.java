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

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.settings.NetworkSettings;

public abstract class AbstractResponseHandler extends MessageHandler 
        implements ResponseHandler {
    
    private long timeout;
    
    public AbstractResponseHandler(Context context) {
        this(context, NetworkSettings.TIMEOUT.getValue());
    }
    
    public AbstractResponseHandler(Context context, long timeout) {
        super(context);
        
        if (timeout < 0L) {
            throw new IllegalArgumentException("Timeout must be >= 0");
        }
        
        this.timeout = timeout;
    }
    
    public long timeout() {
        return timeout;
    }
}
