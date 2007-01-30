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

package org.limewire.mojito.db;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureListener;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.manager.BootstrapManager;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.statistics.DatabaseStatisticContainer;
import org.limewire.mojito.util.DatabaseUtils;
import org.limewire.service.ErrorService;


/**
 * The DHTValueManager periodically publishes all local DHTValues 
 * and evicts expired ones.
 */
public class DHTValueManager implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(DHTValueManager.class);
    
    private Context context;
    
    private RepublishTask republishTask = new RepublishTask();
    
    private ScheduledFuture future;
    
    private DatabaseStatisticContainer databaseStats;
    
    public DHTValueManager(Context context) {
        this.context = context;
        
        databaseStats = context.getDatabaseStats();
    }
    
    /**
     * Starts the DHTValueManager
     */
    public void start() {
        synchronized (republishTask) {
            if (future == null) {
                long delay = DatabaseSettings.REPUBLISH_PERIOD.getValue();
                long initialDelay = delay;
                
                future = context.getDHTExecutorService()
                    .scheduleWithFixedDelay(this, initialDelay, delay, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    /**
     * Stops the DHTValueManager
     */
    public void stop() {
        synchronized (republishTask) {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            
            republishTask.stop();
        }
    }
    
    public void run() {
        synchronized (republishTask) {
            // There is no point in running the DHTValueManager
            // if we're not bootstrapped!
            BootstrapManager bootstrapManager = context.getBootstrapManager();
            synchronized (bootstrapManager) {
                if (!bootstrapManager.isBootstrapped() 
                        || bootstrapManager.isBootstrapping()) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info(context.getName() + " is not bootstrapped");
                    }
                    return;
                }
            }
            
            // Republish but make sure the task from the previous
            // run() has finished as we don't want parallel republishing
            if (republishTask.isDone()) {
                republishTask.republish();
            }
        }
    }
    
    /**
     * The RepublishTask publishes DHTValue(s) one-by-one by going
     * through a List of DHTValues. Everytime a store finishes it
     * continues with the next DHTValue until all DHTValues have
     * been republished
     */
    private class RepublishTask implements DHTFutureListener<StoreResult> {
        
        private Iterator<DHTValueEntity> values = null;
        
        private DHTFuture<StoreResult> future = null;
        
        /**
         * Stops the RepublishTask
         */
        public synchronized void stop() {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            
            values = null;
        }
        
        /**
         * Returns whether or not the RepublishTask is done
         */
        public synchronized boolean isDone() {
            return values == null || !values.hasNext();
        }
        
        /**
         * Starts the republishing
         */
        public synchronized void republish() {
            Database database = context.getDatabase();
            Collection<DHTValueEntity> c = database.values();
            
            if (LOG.isInfoEnabled()) {
                LOG.info(context.getName() + " has " + c.size() + " DHTValues to process");
            }
            
            values = c.iterator();
            next();
        }
        
        /**
         * Republishes the next DHTValue
         */
        private synchronized boolean next() {
            if (isDone()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(context.getName() + " is done with publishing");
                }
                return false;
            }
            
            while(values.hasNext()) {
                DHTValueEntity value = values.next();
                if (manage(value)) {
                    return true;
                }
            }
            
            return false;
        }
        
        /**
         * Publishes or expires the given DHTValue
         */
        private boolean manage(DHTValueEntity entity) {
            
            // Check if value is still in DB because we're
            // working with a copy of the Collection.
            Database database = context.getDatabase();
            synchronized(database) {
                
                if (!database.contains(entity)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(entity + " is no longer stored in our database");
                    }
                    return false;
                }
                
                if (entity.isLocalValue()) {
                    if (!entity.isRepublishingRequired()) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace(entity + " does not require republishing");
                        }
                        return false;
                    }
                } else {
                    if (DatabaseUtils.isExpired(context.getRouteTable(), entity)) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace(entity + " is expired!");
                        }
                        
                        database.remove(entity);
                        databaseStats.EXPIRED_VALUES.incrementStat();
                        return false;
                    }
                }
            }
            
            databaseStats.REPUBLISHED_VALUES.incrementStat();
            
            future = context.store(entity);
            future.addDHTFutureListener(this);
            return true;
        }
        
        public void handleFutureSuccess(StoreResult result) {
            if (LOG.isInfoEnabled()) {
                if (result.getNodes().isEmpty()) {
                    LOG.info("Failed to store " + result.getValues());
                } else {
                    LOG.info(result);
                }
            }
            
            if (!next()) {
                stop();
            }
        }
        
        public void handleFutureFailure(ExecutionException e) {
            LOG.error("ExecutionException", e);

            if (!(e.getCause() instanceof DHTException)) {
                ErrorService.error(e);
            }
            
            if (!next()) {
                stop();
            }
        }

        public void handleFutureCancelled(CancellationException e) {
            LOG.debug("CancellationException", e);
            stop();
        }
        
        public void handleFutureInterrupted(InterruptedException e) {
            LOG.debug("InterruptedException", e);
            stop();
        }   
    }
}
