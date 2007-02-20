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
import java.util.Collections;
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
import org.limewire.mojito.routing.Contact;
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
    
    private final Context context;
    
    private final RepublishTask republishTask = new RepublishTask();
    
    private final DatabaseStatisticContainer databaseStats;
    
    private ScheduledFuture future;
    
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
         * Removes all expired DHTValueEntities from the Database
         */
        private void removeExpired(Collection<? extends DHTValueEntity> entities) {
            Database database = context.getDatabase();
            synchronized (database) {
                for (DHTValueEntity entity : entities) {
                    if (DatabaseUtils.isExpired(context.getRouteTable(), entity)) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace(entity + " is expired!");
                        }
                        
                        database.remove(entity.getKey(), entity.getSecondaryKey());
                        databaseStats.EXPIRED_VALUES.incrementStat();
                    }
                }
            }
        }
        
        /**
         * Starts the republishing
         */
        public synchronized void republish() {
            
            Database database = context.getDatabase();
            synchronized (database) {
                // Remove all values that have expired
                removeExpired(database.values());
            }
            
            // And publish the local values
            DHTValueEntityPublisher valueEntityPublisher = context.getDHTValueEntityPublisher();
            Collection<DHTValueEntity> valuesToPublish = valueEntityPublisher.getValuesToPublish();
            if (valuesToPublish == null) {
                valuesToPublish = Collections.emptyList();
            }
            
            if (LOG.isInfoEnabled()) {
                LOG.info(context.getName() + " has " 
                        + valuesToPublish.size() + " DHTValues to process");
            }
            
            values = valuesToPublish.iterator();
            
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
                DHTValueEntity entry = values.next();
                if (publish(entry)) {
                    return true;
                }
            }
            
            return false;
        }
        
        /**
         * Publishes or expires the given DHTValue
         */
        private boolean publish(DHTValueEntity entity) {
            
            // Check if value is still in DB because we're
            // working with a copy of the Collection.
            databaseStats.REPUBLISHED_VALUES.incrementStat();
            
            future = context.store(entity);
            future.addDHTFutureListener(this);
            return true;
        }
        
        public void handleFutureSuccess(StoreResult result) {
            Collection<? extends Contact> nodes = result.getNodes();
            
            if (!nodes.isEmpty()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(result);
                }
                
                Collection<DHTValueEntity> entities = result.getSucceeded();
                assert (entities.size() == 1);
                DHTValueEntity entity = entities.iterator().next();
                context.getDHTValueEntityPublisher().published(entity);
            } else {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Failed to store " + result.getValues());
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
