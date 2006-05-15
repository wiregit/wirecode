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

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.RequestMessage;


public class StatsRequest extends RequestMessage {
    
    public static final int STATS = 0x00;
    public static final int DB = 0x01;
    public static final int RT = 0x02;
    
    private int request;

    public StatsRequest(int vendor, int version, 
            ContactNode node, KUID messageId, byte[] signature, int request) {
        super(vendor, version, node, messageId, signature);
        
        this.request = request;
    }
    
    public int getRequest() {
        return request;
    }

    public boolean isDBRequest() {
        return (request & DB) == DB;
    }
    
    public boolean isRTRequest() {
        return (request & RT) == RT;
    }
    
}
