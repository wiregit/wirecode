/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.db;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.event.StoreListener;
import de.kapsi.net.kademlia.settings.DatabaseSettings;
import de.kapsi.net.kademlia.settings.KademliaSettings;

public class KeyValuePublisher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(KeyValuePublisher.class);
    
    private final Context context;
    
    private boolean running = false;
    
    private int published = 0;
    private int evicted = 0;
    
    public KeyValuePublisher(Context context) {
        this.context = context;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void stop() {
        running = false;
    }
    
    public void run() {
        if (running) {
            LOG.error("Already running!");
            return;
        }
        
        Iterator it = null;
        Database database = context.getDatabase();
        
        final Object lock = new Object();
        
        running = true;
        while(running) {
            if (it == null) {
                it = database.getAllValues().iterator();
                
                evicted = 0;
                published = 0;
            }
            
            if (it.hasNext()) {
                KeyValue keyValue = (KeyValue)it.next();
                
                synchronized(database) {
                    
                    // this is neccessary because database.getAllValues()
                    // creates a new Collection rather than returning a
                    // reference to the internal data structure.
                    if (!database.contains(keyValue)) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("KeyValue " + keyValue 
                                    + " is no longer stored in our database");
                        }
                        continue;
                    }
                    
                    if (database.isKeyValueExpired(keyValue)) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace(keyValue + " is expired!");
                        }
                        
                        database.remove(keyValue);
                        evicted++;
                        continue;
                    }
                    
                    if (!keyValue.isLocalKeyValue()) {
                        LOG.trace(keyValue + " is not a local value");
                        continue;
                    }
                    
                    if (!database.isRepublishingRequired(keyValue)) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace(keyValue 
                                    + " does not require republishing");
                        }
                        continue;
                    }
                }
                
                synchronized(lock) {
                    try {
                        context.store(keyValue, new StoreListener() {
                            public void store(List keyValues, Collection nodes) {
                                for(Iterator it = keyValues.iterator(); it.hasNext(); ) {
                                    ((KeyValue)it.next()).setRepublishTime(System.currentTimeMillis());
                                    published++;
                                }

                                synchronized(lock) {
                                    lock.notify();
                                }
                                
                                if (LOG.isTraceEnabled()) {
                                    if (!nodes.isEmpty()) {
                                        StringBuffer buffer = new StringBuffer("\nStoring ");
                                        buffer.append(keyValues).append(" at the following Nodes:\n");
                                        
                                        Iterator it = nodes.iterator();
                                        int k = KademliaSettings.getReplicationParameter();
                                        for(int i = 0; i < k && it.hasNext(); i++) {
                                            buffer.append(i).append(": ").append(it.next()).append("\n");
                                        }
                                        
                                        LOG.trace(buffer);
                                        //System.out.println(buffer);
                                    } else {
                                        LOG.trace("Failed to store " + keyValues);
                                    }
                                }
                            }
                        });
                        
                        try {
                            lock.wait();
                        } catch (InterruptedException err) {
                            LOG.error(err);
                        }
                        
                    } catch (IOException err) {
                        LOG.error(err);
                    }
                }
            } else {
                
                it = null;
                try { 
                    Thread.sleep(DatabaseSettings.REPUBLISH_INTERVAL); 
                } catch (InterruptedException err) {
                    LOG.error(err);
                    running = false;
                }
            }
        }
    }
}
