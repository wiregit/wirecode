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

package com.limegroup.mojito.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.event.DHTEventListener;
import com.limegroup.mojito.event.StoreEvent;
import com.limegroup.mojito.event.StoreListener;
import com.limegroup.mojito.handler.response.StoreResponseHandler;

/**
 * 
 */
public class StoreManager extends AbstractManager {
    
    private List<StoreListener> globalListeners = new ArrayList<StoreListener>();
    
    public StoreManager(Context context) {
        super(context);
    }
    
    public void addStoreListener(StoreListener listener) {
        synchronized (globalListeners) {
            globalListeners.add(listener);
        }
    }
    
    public void removeStoreListener(StoreListener listener) {
        synchronized (globalListeners) {
            globalListeners.remove(listener);
        }
    }

    public StoreListener[] getStoreListeners() {
        synchronized (globalListeners) {
            return globalListeners.toArray(new StoreListener[0]);
        }
    }
    
    public DHTFuture<StoreEvent> store(KeyValue keyValue) {
        StoreResponseHandler handler = new StoreResponseHandler(context, keyValue);
        StoreFuture future = new StoreFuture(handler);
        
        context.execute(future);
        return future;
    }
    
    public DHTFuture<StoreEvent> store(Contact node, QueryKey queryKey, KeyValue keyValue) {
        StoreResponseHandler handler = new StoreResponseHandler(context, node, queryKey, keyValue);
        StoreFuture future = new StoreFuture(handler);
        context.execute(future);
        return future;
    }
    
    private class StoreFuture extends FutureTask<StoreEvent> implements DHTFuture<StoreEvent> {
        
        private List<DHTEventListener<StoreEvent>> listeners 
            = new ArrayList<DHTEventListener<StoreEvent>>();
        
        public StoreFuture(Callable<StoreEvent> callable) {
            super(callable);
        }

        public <L extends DHTEventListener<StoreEvent>> 
                void addDHTEventListener(L listener) {
            
            synchronized (listeners) {
                if (isDone()) {
                    try {
                        StoreEvent result = get();
                        listener.handleResult(result);
                    } catch (CancellationException ignore) {
                    } catch (InterruptedException ignore) {
                    } catch (Exception ex) {
                        listener.handleException(ex);
                    }
                } else {
                    listeners.add(listener);
                }
            }
        }

        @Override
        protected void done() {
            super.done();
            
            try {
                StoreEvent entry = get();
                fireResult(entry);
            } catch (CancellationException ignore) {
            } catch (InterruptedException ignore) {
            } catch (Exception err) {
                fireException(err);
            }
        }
        
        private void fireResult(final StoreEvent result) {
            synchronized(globalListeners) {
                for (StoreListener l : globalListeners) {
                    l.handleResult(result);
                }
            }
            
            synchronized(listeners) {
                for (DHTEventListener<StoreEvent> l : listeners) {
                    l.handleResult(result);
                }
            }
        }
        
        private void fireException(final Exception ex) {
            synchronized(globalListeners) {
                for (StoreListener l : globalListeners) {
                    l.handleException(ex);
                }
            }
            
            synchronized(listeners) {
                for (DHTEventListener<StoreEvent> l : listeners) {
                    l.handleException(ex);
                }
            }
        }
    }
}
