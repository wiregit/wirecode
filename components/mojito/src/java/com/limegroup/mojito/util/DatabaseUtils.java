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

package com.limegroup.mojito.util;

import java.util.List;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.settings.KademliaSettings;

/**
 * Miscellaneous untilities for the Database
 */
public class DatabaseUtils {
    
    private DatabaseUtils() {}
    
    /**
     * Returns the expiration time of the given DHTValue
     */
    public static long getExpirationTime(RouteTable routeTable, DHTValue value) {
        // Local DHTValues don't expire
        if (value.isLocalValue()) {
            return Long.MAX_VALUE;
        }
        
        KUID valueId = value.getValueID();
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        List<Contact> nodes = routeTable.select(valueId, k, false);
        
        long creationTime = value.getCreationTime();
        long expirationTime = DatabaseSettings.VALUE_EXPIRATION_TIME.getValue();
        
        // If there are less than k Nodes or the local Node is member
        // of the k-closest Nodes then use the default expiration time
        if (nodes.size() < k || nodes.contains(routeTable.getLocalNode())) {
            return creationTime + expirationTime;
            
        // The value expires inversly proportional otherwise by using
        // the xor distance
        } else {
            KUID valueBucketId = routeTable.getBucketID(valueId);
            KUID localBucketId = routeTable.getBucketID(routeTable.getLocalNode().getNodeID());
            KUID xor = localBucketId.xor(valueBucketId);
            
            int lowestSetBit = xor.toBigInteger().getLowestSetBit();
            float ratio = 0.0f;
            if (lowestSetBit >= 0) {
                ratio = (float)(KUID.LENGTH_IN_BITS - lowestSetBit) / (float)KUID.LENGTH_IN_BITS;
            }
            
            return creationTime + (long)(expirationTime - (expirationTime * ratio));
        }
    }
    
    /**
     * Returns whether or not the given DHTValue has expired
     */
    public static boolean isExpired(RouteTable routeTable, DHTValue value) {
        return System.currentTimeMillis() >= getExpirationTime(routeTable, value);
    }
    
    /**
     * 
     */
    public static boolean isRepublishingRequired(long lastRepublishingTime, int locationCount) {
        long t = (long)((locationCount 
                * DatabaseSettings.VALUE_REPUBLISH_INTERVAL.getValue()) 
                    / KademliaSettings.REPLICATION_PARAMETER.getValue());
        
        // Do never republish more than every X minutes
        long nextPublishTime = Math.max(t, 
                DatabaseSettings.MIN_VALUE_REPUBLISH_INTERVAL.getValue());
        
        long time = lastRepublishingTime + nextPublishTime;
        
        return System.currentTimeMillis() >= time;
    }
}
