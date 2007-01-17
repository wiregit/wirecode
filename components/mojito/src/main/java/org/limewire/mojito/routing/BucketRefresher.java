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

package org.limewire.mojito.routing;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.concurrent.DHTFutureListener;
import org.limewire.mojito.result.FindNodeResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.settings.RouteTableSettings;


/**
 * 
 */
public class BucketRefresher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(BucketRefresher.class);
    
    private Context context;
    
    private RefreshTask refreshTask = new RefreshTask();
    
    private ScheduledFuture future;
    
    public BucketRefresher(Context context) {
        this.context = context;
    }
    
    /**
     * Starts the BucketRefresher
     */
    public void start() {
        synchronized (refreshTask) {
            if (future == null) {
                long delay = RouteTableSettings.RANDOM_REFRESHER_DELAY.getValue();
                long initialDelay = delay;
                
                if (RouteTableSettings.UNIFORM_BUCKET_REFRESH_DISTRIBUTION.getValue()) {
                    initialDelay = delay + (long)(delay * Math.random());
                }
                
                future = context.getDHTExecutorService()
                    .scheduleWithFixedDelay(this, initialDelay, delay, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    /**
     * Stops the BucketRefresher
     */
    public void stop() {
        synchronized (refreshTask) {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            
            refreshTask.stop();
        }
    }
    
    public void run() {
        synchronized (refreshTask) {
            
            if(LOG.isTraceEnabled()) {
                LOG.trace("Random bucket refresh");
            }
            
            // Running the BucketRefresher w/o being bootstrapped is
            // pointless. Try to bootstrap from the RouteTable if 
            // it's possible.
            
            if (!context.isBootstrapped()) {
                if (!context.isBootstrapping()) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Bootstrap " + context.getName());
                    }
                    
                    // If we are not bootstrapped and have some 
                    // Nodes in our RouteTable, try to bootstrap
                    // from the RouteTable
                    
                    DHTFutureListener<PingResult> listener = new DHTFutureAdapter<PingResult>() {
                        @Override
                        public void handleFutureSuccess(PingResult result) {
                            context.bootstrap(result.getContact());
                        }
                    };
                    
                    DHTFuture<PingResult> future = context.findActiveContact();
                    future.addDHTFutureListener(listener);
                } else {
                    if (LOG.isInfoEnabled()) {
                        LOG.info(context.getName() + " is bootstrapping");
                    }
                }
                
                // In any case exit here!
                return;
            }
            
            // Refresh but make sure the task from the previous
            // run() call has finished as we don't want to run
            // refresher tasks in parallel
            if (refreshTask.isDone()) {
                refreshTask.refresh();
            }
        }
    }
    
    /**
     * The RefreshTask iterates one-by-one through a List of KUIDs
     * and does a lookup for the ID. Everytime a lookup finishes it
     * starts a new lookup for the next ID until all KUIDs have been
     * looked up
     */
    private class RefreshTask implements DHTFutureListener<FindNodeResult> {
        
        private Iterator<KUID> bucketIds = null;
        
        private DHTFuture<FindNodeResult> future = null;
        
        /**
         * Returns whether or not the refresh task has 
         * finished (initial state is true)
         */
        public synchronized boolean isDone() {
            return bucketIds == null || !bucketIds.hasNext();
        }
        
        /**
         * Stops the RefreshTask
         */
        public synchronized void stop() {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            
            bucketIds = null;
        }
        
        /**
         * Starts the refresh
         */
        public synchronized boolean refresh() {
            List<KUID> list = context.getRouteTable().getRefreshIDs(false);
            
            if (LOG.isInfoEnabled()) {
                LOG.info(context.getName() + " has " + list.size() + " Buckets to refresh");
            }
            
            bucketIds = list.iterator();
            return next();
        }
        
        /**
         * Lookup the next KUID
         */
        private synchronized boolean next() {
            if (isDone()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(context.getName() + " finished Bucket refreshes");
                }
                return false;
            }
            
            KUID lookupId = bucketIds.next();
            future = context.lookup(lookupId);
            future.addDHTFutureListener(this);
            
            if (LOG.isInfoEnabled()) {
                LOG.info(context.getName() + " started a Bucket refresh lookup for " + lookupId);
            }
            
            return true;
        }
        
        public void handleFutureSuccess(FindNodeResult result) {
            if (LOG.isInfoEnabled()) {
                LOG.info(result);
            }
            
            if (!next()) {
                stop();
            }
        }
        
        public void handleFutureFailure(ExecutionException e) {
            LOG.error("ExecutionException", e);
            
            if (!next()) {
                stop();
            }
        }
        
        public void handleFutureCancelled(CancellationException e) {
            LOG.debug("CancellationException", e);
            stop();
        }
        
        public void handleFutureInterrupted(InterruptedException e) {
            LOG.debug("CancellationException", e);
            stop();
        }
    }
}
