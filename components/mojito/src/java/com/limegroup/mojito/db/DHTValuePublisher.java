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

package com.limegroup.mojito.db;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.event.StoreEvent;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.statistics.DatabaseStatisticContainer;

/**
 * The DHTValuePublisher publishes in periodical intervals
 * all local DHTValues and evicts 
 */
public class DHTValuePublisher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(DHTValuePublisher.class);
    
    private Context context;
    
    private Object lock = new Object();
    
    private DatabaseStatisticContainer databaseStats;
    
    private ScheduledFuture future;
    
    private int published = 0;
    private int evicted = 0;
    
    private long period = 0L;
    
    private long end = 0L;
    
    private static volatile int convoyEffectCounter = 0;
    
    public DHTValuePublisher(Context context) {
        this.context = context;
        
        databaseStats = context.getDatabaseStats();
    }
    
    /**
     * Starts the DHTValuePublisher
     */
    public void start() {
        synchronized (lock) {
            if (future == null) {
                period = DatabaseSettings.REPUBLISH_PERIOD.getValue();
                long delay = period;
                
                end = 0;
                future = context.scheduleAtFixedRate(this, delay, period, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    /**
     * Stops the DHTValuePublisher
     */
    public void stop() {
        synchronized (lock) {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
        }
    }
    
    /**
     * Published the given DHTValue
     */
    private void publish(DHTValue value) throws Exception {
        
        // Check if value is still in DB because we're
        // working with a copy of the Collection.
        Database database = context.getDatabase();
        synchronized(database) {
            
            if (!database.contains(value)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(value + " is no longer stored in our database");
                }
                return;
            }
            
            if (value.isExpired()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(value + " is expired!");
                }
                
                database.remove(value);
                evicted++;
                databaseStats.EXPIRED_VALUES.incrementStat();
                return;
            }
            
            if (!value.isLocalValue()) {
                LOG.trace(value + " is not a local value");
                return;
            }
            
            if (!value.isRepublishingRequired()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(value + " does not require republishing");
                }
                return;
            }
        }
        
        databaseStats.REPUBLISHED_VALUES.incrementStat();
        
        synchronized (lock) {
            if (future == null || future.isCancelled()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Publisher is cancelled");
                }
                return;
            }
        }
        
        StoreEvent evt = context.store(value).get();
        published++;
        
        if (LOG.isTraceEnabled()) {
            if (evt.getNodes().isEmpty()) {
                LOG.trace("Failed to store " + value);
            } else {
                LOG.trace(evt.toString());
            }
        }
    }
    
    public void run() {
        
        if (!context.isBootstrapped() || context.isBootstrapping()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Skipping this republishing interval 'cause we're " 
                        + "either not bootstrapped or we're bootstrapping: " 
                        + context.isBootstrapped() + "/" + context.isBootstrapping());
            }
            return;
        }
        
        // Scheduled Tasks have the side effect that if the last
        // iteration took longer than the schedule period it will
        // re-shedule the task immediately causing the Task being
        // executed continuously without a delay. 
        long timeSinceLastRepublish = System.currentTimeMillis() - end;
        if (timeSinceLastRepublish < period) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Skipping this republishing interval: " 
                        + timeSinceLastRepublish + " < " + period);
            }
            return;
        }
        
        published = 0;
        evicted = 0;
        
        synchronized (getClass()) {
            convoyEffectCounter++;
            System.out.println("DHTValuePublisher: " + convoyEffectCounter);
        }
        
        List<DHTValue> values = null;
        Database database = context.getDatabase();
        synchronized (database) {
            values = new ArrayList<DHTValue>(database.values());
        }
        
        for(DHTValue value : values) {
            if (context.isBootstrapping()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(context.getName() + " is bootstrapping, interrupting publisher");
                }
                break;
            }
            
            try {
                publish(value);
            } catch (Exception err) {
                LOG.error("Exception", err);
            }
        }
        
        synchronized (getClass()) {
            convoyEffectCounter--;
        }
        
        end = System.currentTimeMillis();
    }
}
