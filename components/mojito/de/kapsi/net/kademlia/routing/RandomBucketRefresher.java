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
 
package de.kapsi.net.kademlia.routing;

import java.io.IOException;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;

public class RandomBucketRefresher extends TimerTask implements Runnable{
    
    private static final Log LOG = LogFactory.getLog(RandomBucketRefresher.class);
    
    private Context context;
    
    public RandomBucketRefresher(Context context) {
        this.context = context;
    }
    
    public void run() {
        if(LOG.isTraceEnabled()) {
            LOG.trace("Random bucket refresh");
        }
        
        try {
            context.getRouteTable().refreshBuckets(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
