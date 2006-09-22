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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.event.DHTEventListener;

/**
 * An abstract class for various types of Managers.
 */
abstract class AbstractManager<V> {
    
    protected final Context context;
    
    private List<DHTEventListener<V>> globalListeners 
        = new CopyOnWriteArrayList<DHTEventListener<V>>();
    
    public AbstractManager(Context context) {
        this.context = context;
    }
    
    public void addDHTEventListener(DHTEventListener<V> listener) {
        globalListeners.add(listener);
    }
    
    public void removeDHTEventListener(DHTEventListener<V> listener) {
        globalListeners.remove(listener);
    }

    public DHTEventListener[] getDHTEventListeners() {
        return globalListeners.toArray(new DHTEventListener[0]);
    }
    
    protected abstract class AbstractDHTFuture extends FutureTask<V> implements DHTFuture<V> {
        
        private List<DHTEventListener<V>> listeners 
            = new ArrayList<DHTEventListener<V>>();
        
        public AbstractDHTFuture(Callable<V> callable) {
            super(callable);
        }
        
        public void addDHTEventListener(DHTEventListener<V> listener) {
            if (listener == null) {
                return;
            }
            
            if (isCancelled()) {
                return;
            }
            
            synchronized (listeners) {
                if (isDone()) {
                    try {
                        V result = get();
                        listener.handleResult(result);
                    } catch (InterruptedException ignore) {
                    } catch (ExecutionException ex) {
                        Throwable cause = ex.getCause();
                        if (!(cause instanceof CancellationException)) {
                            listener.handleThrowable(cause);
                        }
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
            } catch (InterruptedException ignore) {
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (!(cause instanceof CancellationException)) {
                    fireThrowable(cause);
                }
            }
        }
        
        protected void fireResult(V result) {
            for (DHTEventListener<V> l : globalListeners) {
                l.handleResult(result);
            }
            
            synchronized(listeners) {
                for (DHTEventListener<V> l : listeners) {
                    l.handleResult(result);
                }
            }
        }
        
        protected void fireThrowable(Throwable ex) {
            for (DHTEventListener<V> l : globalListeners) {
                l.handleThrowable(ex);
            }
            
            synchronized(listeners) {
                for (DHTEventListener<V> l : listeners) {
                    l.handleThrowable(ex);
                }
            }
        }
    }
}
