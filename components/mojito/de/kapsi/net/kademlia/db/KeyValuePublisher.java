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
    
    private static final long LOCK_POLLING_INTERVAL = 100L;
    
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
        
        final WaitLock lock = new WaitLock(DatabaseSettings.REPUBLISH_INTERVAL, LOCK_POLLING_INTERVAL);
        
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
                
                try {
                    lock.lock();
                    context.store(value, new StoreListener() {
                        public void store(KeyValue keyValue, Collection nodes) {
                            keyValue.setUpdateTime(System.currentTimeMillis());
                            published++;
                            lock.release();
                            
                            if (LOG.isTraceEnabled()) {
                                if (!nodes.isEmpty()) {
                                    StringBuffer buffer = new StringBuffer("\nStoring ");
                                    buffer.append(keyValue).append(" at the following Nodes:\n");
                                    
                                    Iterator it = nodes.iterator();
                                    int k = KademliaSettings.getReplicationParameter();
                                    for(int i = 0; i < k && it.hasNext(); i++) {
                                        buffer.append(i).append(": ").append(it.next()).append("\n");
                                    }
                                    LOG.trace(buffer);
                                } else {
                                    LOG.trace("Failed to store " + keyValue);
                                }
                            }
                        }
                    });
                } catch (IOException err) {
                    LOG.error(err);
                }
                
                try {
                    lock.doWait();
                } catch (InterruptedException err) {
                    LOG.error(err);
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
    
    private static final class WaitLock {
        
        private boolean waitFlag = false;
        
        private final long timeout;
        private final long wait;
        
        private final Object lock = new Object();
        
        private WaitLock(long timeout, long wait) {
            this.timeout = timeout;
            this.wait = wait;
        }
        
        public void release() {
            waitFlag = false;
        }
        
        public void lock() {
            waitFlag = true;
        }
        
        public void doWait() throws InterruptedException {
            synchronized(lock) {
                long t = System.currentTimeMillis() + timeout;
                while(waitFlag) {
                    if (System.currentTimeMillis() >= t) {
                        throw new InterruptedException("Timeout");
                    }
                    lock.wait(wait);
                }
            }
        }
    }
}
