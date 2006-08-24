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
 
package com.limegroup.mojito.routing;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.FindNodeEvent;
import com.limegroup.mojito.handler.response.FindNodeResponseHandler;
import com.limegroup.mojito.settings.RouteTableSettings;

/**
 * The RandomBucketRefresher class calls in periodical intervals
 * RouteTable.refreshBuckets()
 */
public class RandomBucketRefresher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(RandomBucketRefresher.class);
    
    private Context context;
    
    private volatile ScheduledFuture future;
    
    private long period = 0L;
    
    private long end = 0L;
    
    public RandomBucketRefresher(Context context) {
        this.context = context;
    }
    
    public synchronized void start() {
        if (future == null) {
            period = RouteTableSettings.BUCKET_REFRESH_PERIOD.getValue();
            long delay = period;
            
            if (RouteTableSettings.UNIFORM_BUCKET_REFRESH_DISTRIBUTION.getValue()) {
                delay = period + (long)(period * Math.random());
            }
            
            end = 0L;
            future = context.scheduleAtFixedRate(this, delay, period, TimeUnit.MILLISECONDS);
        }
    }
    
    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }
    
    public void run() {
        if(LOG.isTraceEnabled()) {
            LOG.trace("Random bucket refresh");
        }
        
        ScheduledFuture f = future;
        if (f == null || f.isCancelled()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("RandomBucketRefresher is canceled");
            }
            return;
        }
        
        if (!context.isBootstrapped()) {
            if (context.isBootstrapping()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(context.getName() + " is bootstrapping, not running refresher");
                }
            } else {
                // If we are not bootstrapped and have got some 
                // Nodes in our RouteTable, try bootstrapping
                // this will take some MRS nodes and bootstraps
                // from them.
                context.bootstrap(); 
            }
            return;
        }
        
        // Scheduled Tasks have the side effect that if the last
        // iteration took longer than the schedule period it will
        // re-shedule the task immediately causing the Task being
        // executed continuously without a delay. 
        long timeSinceLastRefresh = System.currentTimeMillis() - end;
        if (timeSinceLastRefresh < period) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Skipping this refresh interval: " 
                        + timeSinceLastRefresh + " < " + period);
            }
            return;
        }
        
        List<KUID> ids = context.getRouteTable().getRefreshIDs(false);
        
        try {
            for(Iterator<KUID> it = ids.iterator(); it.hasNext(); ) {
                KUID nodeId = it.next();
                
                try {
                    
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Starting bucket refresh lookup: "+ nodeId);
                    }
                    
                    FindNodeResponseHandler handler 
                        = new FindNodeResponseHandler(context, nodeId);
                    
                    FindNodeEvent event = handler.call();
                    
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Finished refresh with " + event.getNodes().size() + " nodes");
                    }
                    
                } finally {
                    it.remove();
                }
            }
        } catch (Exception err) {
            LOG.error("Exception", err);
        }
    }
}
