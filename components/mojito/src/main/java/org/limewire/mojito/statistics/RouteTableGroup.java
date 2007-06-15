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

package org.limewire.mojito.statistics;

import org.limewire.mojito.routing.RouteTable.RouteTableEvent;
import org.limewire.mojito.routing.RouteTable.RouteTableListener;
import org.limewire.mojito.routing.RouteTable.RouteTableEvent.EventType;

public class RouteTableGroup extends StatisticsGroup implements RouteTableListener {

    private final Statistic<Long> liveCount = new Statistic<Long>();
    
    private final Statistic<Long> unknownCount = new Statistic<Long>();
    
    private final Statistic<Long> cachedCount = new Statistic<Long>();
    
    private final Statistic<Long> removedDead = new Statistic<Long>();
    
    private final Statistic<Long> bucketCount = new Statistic<Long>();
    
    public void handleRouteTableEvent(RouteTableEvent event) {
        EventType type = event.getEventType();
        
        if (type == EventType.ADD_ACTIVE_CONTACT) {
            if (event.getContact().isAlive()) {
                liveCount.incrementByOne();
            } else {
                unknownCount.incrementByOne();
            }
            
        } else if (type == EventType.ADD_CACHED_CONTACT) {
            cachedCount.incrementByOne();
            
        } else if (type == EventType.REMOVE_CONTACT) {
            if (event.getContact().isDead()) {
                removedDead.incrementByOne();
            }
            
        } else if (type == EventType.REPLACE_CONTACT) {
            liveCount.incrementByOne();
            
            if (event.getContact().isDead()) {
                removedDead.incrementByOne();
            }
            
        } else if (type == EventType.SPLIT_BUCKET) {
            // Increment only by one 'cause splitting a Bucket
            // creates only one new Bucket
            bucketCount.incrementByOne();
        }
    }
}
