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
import java.util.concurrent.FutureTask;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.event.DHTEventListener;

/**
 * 
 */
abstract class AbstractManager<V> {
    
    protected final Context context;
    
    private List<DHTEventListener<V>> globalListeners 
        = new ArrayList<DHTEventListener<V>>();
    
    public AbstractManager(Context context) {
        this.context = context;
    }
    
    public <L extends DHTEventListener<V>> void addDHTEventListener(L listener) {
        synchronized (globalListeners) {
            globalListeners.add(listener);
        }
    }
    
    public <L extends DHTEventListener<V>> void removeDHTEventListener(L listener) {
        synchronized (globalListeners) {
            globalListeners.remove(listener);
        }
    }

    public DHTEventListener[] getDHTEventListeners() {
        synchronized (globalListeners) {
            return globalListeners.toArray(new DHTEventListener[0]);
        }
    }
    
    protected abstract class AbstractDHTFuture extends FutureTask<V> implements DHTFuture<V> {
        
        private List<DHTEventListener<V>> listeners 
            = new ArrayList<DHTEventListener<V>>();
        
        public AbstractDHTFuture(Callable<V> callable) {
            super(callable);
        }
        
        public <L extends DHTEventListener<V>> void addDHTEventListener(L listener) {
            synchronized (listeners) {
                if (isDone()) {
                    try {
                        V result = get();
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

        protected abstract void deregister();
        
        @Override
        protected void done() {
            super.done();
            
            deregister();
            
            try {
                V result = get();
                fireResult(result);
            } catch (CancellationException ignore) {
            } catch (InterruptedException ignore) {
            } catch (Exception ex) {
                fireException(ex);
            }
        }
        
        protected void fireResult(V result) {
            synchronized(globalListeners) {
                for (DHTEventListener<V> l : globalListeners) {
                    l.handleResult(result);
                }
            }
            
            synchronized(listeners) {
                for (DHTEventListener<V> l : listeners) {
                    l.handleResult(result);
                }
            }
        }
        
        protected void fireException(Exception ex) {
            synchronized(globalListeners) {
                for (DHTEventListener<V> l : globalListeners) {
                    l.handleException(ex);
                }
            }
            
            synchronized(listeners) {
                for (DHTEventListener<V> l : listeners) {
                    l.handleException(ex);
                }
            }
        }
    }
}
