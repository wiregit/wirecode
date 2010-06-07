package org.limewire.mojito;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.collection.IdentityHashSet;
import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTValueFuture;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.io.StoreResponseHandler;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.StoreSettings;
import org.limewire.mojito.storage.Value;
import org.limewire.mojito.storage.ValueTuple;
import org.limewire.mojito.util.EntryImpl;
import org.limewire.security.SecurityToken;
import org.limewire.util.ExceptionUtils;

/**
 * The {@link StoreManager} is responsible for storing values in the DHT.
 */
class StoreManager implements Closeable {
    
    private final DefaultDHT dht;
    
    private final Queue queue;
    
    /**
     * Creates a {@link StoreManager}.
     */
    public StoreManager(DefaultDHT dht) {
        this (dht, StoreSettings.PARALLEL_STORES.getValue());
    }
    
    /**
     * Creates a {@link StoreManager}.
     */
    public StoreManager(DefaultDHT dht, int concurrency) {
        this.dht = dht;
        this.queue = new Queue(concurrency);
    }
    
    @Override
    public void close() {
        queue.close();
    }
    
    /**
     * Clears the {@link StoreManager}'s queue.
     */
    public void clear() {
        queue.clear();
    }
    
    /**
     * Stores the given key-value pair.
     */
    public DHTFuture<StoreEntity> put(KUID key, Value value, 
            long timeout, TimeUnit unit) {
        
        ValueTuple entity = ValueTuple.createValueTuple(
                dht, key, value);
        return put(entity, timeout, unit);
    }
    
    /**
     * Stores the given key-value pair.
     */
    private DHTFuture<StoreEntity> put(ValueTuple value, 
            long timeout, TimeUnit unit) {
        
        // The store operation is a composition of two DHT operations.
        // In the first step we're doing a FIND_NODE lookup to find
        // the K-closest nodes to a given key. In the second step we're
        // performing a STORE operation on the K-closest nodes we found.
        
        final Object lock = new Object();
        
        synchronized (lock) {
            
            // The DHTFuture for the STORE operation. We initialize it 
            // at the very end of this block of code.
            final AtomicReference<DHTFuture<StoreEntity>> futureRef 
                = new AtomicReference<DHTFuture<StoreEntity>>();
            
            final StoreResponseHandler process 
                = new StoreResponseHandler(dht, value, timeout, unit);
            
            KUID lookupId = value.getPrimaryKey();
            final DHTFuture<NodeEntity> lookup 
                = dht.lookup(lookupId, timeout, unit);
            
            lookup.addFutureListener(new EventListener<FutureEvent<NodeEntity>>() {
                @Override
                public void handleEvent(FutureEvent<NodeEntity> event) {
                    synchronized (lock) {
                        try {
                            // The reference can be null if the FutureManager was
                            // unable to execute the STORE process. This can happen
                            // if the Context is being shutdown.
                            
                            DHTFuture<StoreEntity> future = futureRef.get();
                            if (future != null && !future.isDone()) {
                                switch (event.getType()) {
                                    case SUCCESS:
                                        onSuccess(event.getResult());
                                        break;
                                    case EXCEPTION:
                                        onException(event.getException());
                                        break;
                                    default:
                                        onCancellation();
                                        break;
                                }
                            }
                        } catch (Throwable t) {
                            uncaughtException(t);
                        }
                    }
                }
                
                private void onSuccess(NodeEntity entity) 
                        throws IOException {
                    process.store(entity.getContacts());
                }
                
                private void onException(Throwable t) {
                    futureRef.get().setException(t);
                }
                
                private void onCancellation() {
                    futureRef.get().cancel(true);
                }
                
                private void uncaughtException(Throwable t) {
                    onException(t);
                    ExceptionUtils.reportIfUnchecked(t);
                }
            });
            
            DHTFuture<StoreEntity> store 
                = dht.submit(process, timeout, unit);
            futureRef.set(store);
            
            store.addFutureListener(new EventListener<FutureEvent<StoreEntity>>() {
                @Override
                public void handleEvent(FutureEvent<StoreEntity> event) {
                    lookup.cancel(true);
                }
            });
            
            return store;
        }
    }
    
    @SuppressWarnings("unchecked")
    private DHTFuture<StoreEntity> store(Contact dst, 
            SecurityToken securityToken, ValueTuple entity, 
            long timeout, TimeUnit unit) {
        
        Entry<Contact, SecurityToken>[] contacts = new Entry[] { 
            new EntryImpl<Contact, SecurityToken>(dst, securityToken) 
        };
        
        StoreResponseHandler process = new StoreResponseHandler(
                dht, contacts, entity, timeout, unit);
        
        return dht.submit(process, timeout, unit);
    }
    
    /**
     * Enqueues the given key-value pair.
     */
    public DHTFuture<StoreEntity> enqueue(KUID key, Value value, 
            long timeout, TimeUnit unit) {
        
        return queue.enqueue(key, value, timeout, unit);
    }
    
    /**
     * Enqueues the given key-value pair.
     */
    public DHTFuture<StoreEntity> enqueue(Contact dst, 
            SecurityToken securityToken, ValueTuple entity, 
            long timeout, TimeUnit unit) {
        return queue.enqueue(dst, securityToken, entity, timeout, unit);
    }
    
    /**
     * The {@link Queue} manages parallel store-operations.
     */
    private class Queue implements Closeable {
        
        private final int concurrency;
        
        private final List<FutureHandle> queue 
            = new ArrayList<FutureHandle>();
        
        private final Set<FutureHandle> active 
            = new IdentityHashSet<FutureHandle>();
        
        public Queue(int concurrency) {
            this.concurrency = concurrency;
        }
        
        @Override
        public synchronized void close() {
            clear();
        }
        
        /**
         * Clears the queue and cancels active processes.
         */
        public synchronized void clear() {
            for (FutureHandle handle : active) {
                handle.cancel();
            }
            
            for (FutureHandle handle : queue) {
                handle.cancel();
            }
            
            active.clear();
            queue.clear();
        }
        
        /**
         * Enqueues the given key-value.
         */
        public DHTFuture<StoreEntity> enqueue(KUID key, Value value, 
                long timeout, TimeUnit unit) {
            
            ValueTuple entity = ValueTuple.createValueTuple(
                    dht, key, value);
            return enqueue(entity, timeout, unit);
        }
        
        /**
         * Enqueues the given key-value.
         */
        private DHTFuture<StoreEntity> enqueue(final ValueTuple entity, 
                final long timeout, final TimeUnit unit) {
            
            FutureHandle handle = new FutureHandle() {
                @Override
                protected DHTFuture<StoreEntity> createFuture() {
                    return StoreManager.this.put(entity, timeout, unit);
                }
            };
            
            return enqueue(handle);
        }
        
        /**
         * Enqueues the given key-value.
         */
        public DHTFuture<StoreEntity> enqueue(final Contact dst, 
                final SecurityToken securityToken, final ValueTuple entity, 
                final long timeout, final TimeUnit unit) {
            
            FutureHandle handle = new FutureHandle() {
                @Override
                protected DHTFuture<StoreEntity> createFuture() {
                    return StoreManager.this.store(dst, 
                            securityToken, entity, timeout, unit);
                }
            };
            
            return enqueue(handle);
        }
        
        /**
         * Enqueues the given key-value.
         */
        private synchronized DHTFuture<StoreEntity> enqueue(FutureHandle handle) {
            queue.add(handle);
            doNext();
            return handle.externalFuture;
        }
        
        /**
         * Stores the next value from the queue.
         */
        private synchronized void doNext() {
            while (active.size() < concurrency && !queue.isEmpty()) {
                FutureHandle handle = queue.remove(0);
                handle.store();
                active.add(handle);
            }
        }
        
        /**
         * Called if the given {@link FutureHandle} completed.
         */
        private synchronized void complete(FutureHandle handle) {
            active.remove(handle);
            doNext();
        }
        
        /**
         * A handle that keeps track of the external and 
         * internal {@link DHTFuture}s.
         */
        private abstract class FutureHandle {
            
            /** 
             * This {@link DHTFuture} is being given to the user.
             */
            private final DHTFuture<StoreEntity> externalFuture 
                = new DHTValueFuture<StoreEntity>();
            
            private DHTFuture<StoreEntity> future;
            
            private boolean cancelled = false;
            
            public FutureHandle() {
                externalFuture.addFutureListener(
                        new EventListener<FutureEvent<StoreEntity>>() {
                    @Override
                    public void handleEvent(FutureEvent<StoreEntity> event) {
                        complete();
                    }
                });
            }
            
            private void complete() {
                cancel();
                Queue.this.complete(this);
            }
            
            public synchronized void cancel() {
                if (!cancelled) {
                    cancelled = true;
                    
                    if (future != null) {
                        future.cancel(true);
                    }
                    
                    onCancellation();
                }
            }
            
            /**
             * Called if a value was stored successfully.
             */
            private void onSuccess(StoreEntity entity) {
                externalFuture.setValue(entity);
            }
            
            /**
             * Called if an {@link Exception} occurred.
             */
            private void onException(Throwable t) {
                externalFuture.setException(t);
            }
            
            /**
             * Called if the store task was cancelled.
             */
            private void onCancellation() {
                externalFuture.cancel(true);
            }
            
            /**
             * Called if an uncaught {@link Exception} occurred.
             */
            private void uncaughtException(Throwable t) {
                onException(t);
                ExceptionUtils.reportIfUnchecked(t);
            }
            
            /**
             * Creates and returns a {@link DHTFuture}.
             */
            protected abstract DHTFuture<StoreEntity> createFuture();
            
            public synchronized void store() {
                if (cancelled) {
                    return;
                }
                
                future = createFuture();
                future.addFutureListener(new EventListener<FutureEvent<StoreEntity>>() {
                    @Override
                    public void handleEvent(FutureEvent<StoreEntity> event) {
                        synchronized (FutureHandle.this) {
                            try {
                                switch (event.getType()) {
                                    case SUCCESS:
                                        onSuccess(event.getResult());
                                        break;
                                    case EXCEPTION:
                                        onException(event.getException());
                                        break;
                                    case CANCELLED:
                                        onCancellation();
                                        break;
                                }
                            } catch (Throwable t) {
                                uncaughtException(t);
                            }
                        }
                    }
                });
            }
        }
    }
}
