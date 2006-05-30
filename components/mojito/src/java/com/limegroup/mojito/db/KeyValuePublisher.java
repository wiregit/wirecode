/*
 * Mojito Distributed Hash Tabe (DHT)
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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.event.StoreListener;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.statistics.DataBaseStatisticContainer;
import com.limegroup.mojito.util.CollectionUtils;

/**
 * The KeyValuePublisher class republishes local KeyValue on the DHT
 */
public class KeyValuePublisher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(KeyValuePublisher.class);
    
    private Context context;
    private Database database;
    
    private final DataBaseStatisticContainer databaseStats;
    
    private boolean running = false;
    
    private int published = 0;
    private int evicted = 0;
    
    private Object publishLock = new Object();
    
    public KeyValuePublisher(Context context) {
        this.context = context;
        this.database = context.getDatabase();
        
        databaseStats = context.getDataBaseStats();
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void stop() {
        synchronized (publishLock) {
            running = false;
            publishLock.notify();
        }
    }
    
    private void publishKeyValue(KeyValue keyValue) {
        
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
        
        if (!isRunning()) {
            return;
        }
        
        databaseStats.REPUBLISHED_VALUES.incrementStat();
        
        try {
            context.store(keyValue, new StoreListener() {
                public void store(KeyValue keyValue, Collection nodes) {
                    keyValue.setLastPublishTime(System.currentTimeMillis());
                    keyValue.setNumLocs(nodes.size());
                    published++;

                    synchronized(publishLock) {
                        publishLock.notify();
                    }
                    
                    if (LOG.isTraceEnabled()) {
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
            });
            
            try {
                publishLock.wait();
            } catch (InterruptedException ignore) {}
            
        } catch (IOException err) {
            LOG.error("KeyValuePublisher IO exception: ", err);
        }
    }
    
    public void run() {
        synchronized (publishLock) {
            if (running) {
                LOG.error("Already running!");
                return;
            }
            running = true;
        }
        
        Iterator it = null;
        Database database = context.getDatabase();
        
        while(true) {
            synchronized (publishLock) {
                if (!running) {
                    break;
                }
            
                if (context.isBootstrapped()) {
                    if (it == null) {
                        it = database.getValues().iterator();
                        
                        evicted = 0;
                        published = 0;
                    }
                }
                
                if (!running) {
                    break;
                }
                
                if (it != null && it.hasNext()) {
                    KeyValue keyValue = (KeyValue)it.next();
                    publishKeyValue(keyValue);
                } else {
                    it = null;
                    
                    try {
                        publishLock.wait(DatabaseSettings.RUN_REPUBLISHER_EVERY.getValue());
                    } catch (InterruptedException ignore) {}
                }
            }
        }
    }
}
