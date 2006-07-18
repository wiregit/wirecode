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
 
package com.limegroup.mojito;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;

import com.limegroup.mojito.Contact.State;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.impl.ContactNode;

class MojitoHelper {
    
    private MojitoHelper() {}
    
    public static Context getContext(MojitoDHT dht) {
        try {
            Field context = MojitoDHT.class.getDeclaredField("context");
            context.setAccessible(true);
            return (Context)context.get(dht);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void setNodeID(MojitoDHT dht, KUID nodeId) {
        setNodeID(getContext(dht), nodeId);
    }
    
    public static void setNodeID(Context context, KUID nodeId) {
        RouteTable routeTable = context.getRouteTable();
        routeTable.clear();
        
        int vendor = context.getVendor();
        int version = context.getVersion();
        
        Contact node = new ContactNode(vendor, version, nodeId, new InetSocketAddress(0), State.UNKNOWN);
        node.setTimeStamp(Long.MAX_VALUE);
        routeTable.add(node);
        
        try {
            Field localNode = Context.class.getDeclaredField("localNode");
            localNode.setAccessible(true);
            localNode.set(context, node);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
