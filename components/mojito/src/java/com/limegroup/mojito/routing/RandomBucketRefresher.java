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

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.concurrent.DHTFutureAdapter;
import com.limegroup.mojito.concurrent.DHTFutureListener;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.handler.response.FindNodeResponseHandler;
import com.limegroup.mojito.result.FindNodeResult;
import com.limegroup.mojito.result.PingResult;
import com.limegroup.mojito.settings.RouteTableSettings;

/**
 * The RandomBucketRefresher class calls in periodical intervals
 * RouteTable.refreshBuckets()
 */
public class RandomBucketRefresher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(RandomBucketRefresher.class);
    
    private Context context;
    
    private volatile ScheduledFuture future;
    
    private long delay = 0L;
    
    public RandomBucketRefresher(Context context) {
        this.context = context;
    }
    
    public synchronized void start() {
        if (future == null) {
            delay = RouteTableSettings.RANDOM_REFRESHER_DELAY.getValue();
            long initialDelay = delay;
            
            if (RouteTableSettings.UNIFORM_BUCKET_REFRESH_DISTRIBUTION.getValue()) {
                initialDelay = delay + (long)(delay * Math.random());
            }
            
            future = context.getDHTExecutorService()
                .scheduleWithFixedDelay(this, initialDelay, delay, TimeUnit.MILLISECONDS);
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
                
                DHTFutureListener<PingResult> listener = new DHTFutureAdapter<PingResult>() {
                    @Override
                    public void handleFutureSuccess(PingResult result) {
                        context.bootstrap(result.getContact());
                    }
                };
                
                DHTFuture<PingResult> future = context.findActiveContact();
                future.addDHTFutureListener(listener);
            }
            return;
        }
        
        List<KUID> ids = context.getRouteTable().getRefreshIDs(false);
        
        /*if (routingStats != null) {
            routingStats.BUCKET_REFRESH_COUNT.addData(ids.size());
        }*/
        
        try {
            for(int i = 0; i < ids.size(); i++) {
                KUID nodeId = ids.get(i);
                
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Starting bucket refresh lookup ("+(i+1)+"/"+ids.size()+"): " + nodeId);
                }
                
                FindNodeResponseHandler handler 
                    = new FindNodeResponseHandler(context, nodeId);
                
                try {
                    FindNodeResult event = handler.call();
                    
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Finished refresh ("+(i+1)+"/"+ids.size()+") with " 
                                + event.getNodes().size() + " nodes");
                    }
                } catch (DHTException err) {
                    LOG.error("DHTException", err);
                    // Continue with refrshing!
                }
            }
        } catch (InterruptedException err) {
            LOG.info("InterruptedException", err);
        }
    }
}
