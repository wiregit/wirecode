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

import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.statistics.DatabaseStatisticContainer;
import com.limegroup.mojito.util.CollectionUtils;

// TODO rename to Publisher?
public class KeyValuePublisher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(KeyValuePublisher.class);
    
    private Context context;
    private Database database;
    
    private DatabaseStatisticContainer databaseStats;
    
    private ScheduledFuture future;
    
    private int published = 0;
    private int evicted = 0;
    
    private Object lock = new Object();
    
    public KeyValuePublisher(Context context) {
        this.context = context;
        this.database = context.getDatabase();
        
        databaseStats = context.getDatabaseStats();
    }
    
    public void start() {
        synchronized (lock) {
            if (future == null) {
                future = context.scheduleAtFixedRate(this, 
                        DatabaseSettings.RUN_REPUBLISHER_EVERY.getValue(), 
                        DatabaseSettings.RUN_REPUBLISHER_EVERY.getValue());
            }
        }
    }
    
    public void stop() {
        synchronized (lock) {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
        }
    }
    
    private void publish(KeyValue keyValue) throws Exception {
        
        // Check if KeyValue is still in DB because we're
        // working with a copy of the Collection.
        synchronized(database) {
            
            if (!database.contains(keyValue)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("KeyValue " + keyValue 
                            + " is no longer stored in our database");
                }
                return;
            }
            
            if (database.isKeyValueExpired(keyValue)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(keyValue + " is expired!");
                }
                
                database.remove(keyValue);
                evicted++;
                databaseStats.EXPIRED_VALUES.incrementStat();
                return;
            }
            
            if (!keyValue.isLocalKeyValue()) {
                LOG.trace(keyValue + " is not a local value");
                return;
            }
            
            if (!database.isRepublishingRequired(keyValue)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(keyValue 
                            + " does not require republishing");
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
        
        Entry<KeyValue, List<Contact>> entry = context.store(keyValue).get();
        published++;
        
        if (LOG.isTraceEnabled()) {
            List<Contact> nodes = entry.getValue();
            if (!nodes.isEmpty()) {
                StringBuffer buffer = new StringBuffer("\nStoring ");
                buffer.append(keyValue).append(" at the following Nodes:\n");
                buffer.append(CollectionUtils.toString(nodes));
                LOG.trace(buffer);
            } else {
                LOG.trace("Failed to store " + keyValue);
            }
        }
    }

    public void run() {
        
        published = 0;
        evicted = 0;
        
        for(KeyValue keyValue : database.getValues()) {
            if (context.isBootstrapping()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(context.getName() + " is bootstrapping, interrupting publisher");
                }
                break;
            }
            
            try {
                publish(keyValue);
            } catch (Exception err) {
                LOG.error("Exception", err);
            }
        }
    }
}
