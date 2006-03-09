/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.db;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Collection;
import java.util.Iterator;

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
                it = database.getValues().iterator();
                
                evicted = 0;
                published = 0;
            }
            
            if (it.hasNext()) {
                KeyValue value = (KeyValue)it.next();
                
                synchronized(database) {
                    if (!database.contains(value)) {
                        continue;
                    }
                    
                    try {
                        if (database.isExpired(value)) {
                            if (database.isLocalValue(value)) {
                                if (LOG.isTraceEnabled()) {
                                    LOG.trace("Renewing CreationTime lease of " + value);
                                }
                                
                                // TODO see comment of KeyValue.setCreationTime!!!
                                value.setCreationTime(System.currentTimeMillis());
                                
                            } else {
                                if (LOG.isTraceEnabled()) {
                                    LOG.trace(value + " is expired!");
                                }
                                
                                database.remove(value);
                                evicted++;
                                continue;
                            }
                        }
                    } catch (SignatureException err) {
                        LOG.error(err);
                        continue;
                    } catch (InvalidKeyException err) {
                        LOG.error(err);
                        continue;
                    }
                    
                    if (!database.isUpdateRequired(value)) {
                        continue;
                    }
                }
                
                synchronized(lock) {
                    try {
                        context.store(value, new StoreListener() {
                            public void store(KeyValue keyValue, Collection nodes) {
                                keyValue.setUpdateTime(System.currentTimeMillis());
                                published++;
                                
                                synchronized(lock) {
                                    lock.notify();
                                }
                                
                                if (true || LOG.isTraceEnabled()) {
                                    if (!nodes.isEmpty()) {
                                        StringBuffer buffer = new StringBuffer("\nStoring ");
                                        buffer.append(keyValue).append(" at the following Nodes:\n");
                                        
                                        Iterator it = nodes.iterator();
                                        int k = KademliaSettings.getReplicationParameter();
                                        for(int i = 0; i < k && it.hasNext(); i++) {
                                            buffer.append(i).append(": ").append(it.next()).append("\n");
                                        }
                                        //LOG.trace(buffer);
                                        System.out.println(buffer);
                                    } else {
                                        LOG.trace("Failed to store " + keyValue);
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
