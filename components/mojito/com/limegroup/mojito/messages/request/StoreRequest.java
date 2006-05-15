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
 
package com.limegroup.mojito.messages.request;

import java.util.Collection;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.RequestMessage;


public class StoreRequest extends RequestMessage {

    private int remaining;
    
    private QueryKey queryKey;
    private Collection values;
    
    public StoreRequest(int vendor, int version, 
            ContactNode node, KUID messageId, int remaining, 
            QueryKey queryKey, Collection values) {
        super(vendor, version, node, messageId);
        
        if (remaining < 0 || remaining > 0xFFFF) {
            throw new IllegalArgumentException("Remaining: " + remaining);
        }
        
        this.remaining = remaining;
        
        this.queryKey = queryKey;
        this.values = values;
    }

    public QueryKey getQueryKey() {
        return queryKey;
    }
    
    public int getRemaingCount() {
        return remaining;
    }
    
    public Collection getValues() {
        return values;
    }
    
    public String toString() {
        return values.toString();
    }
}
