/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

package org.limewire.mojito.concurrent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;


/**
 * An abstract implementation of DHTFuture
 */
public class DHTFutureTask<V> extends FutureTask<V> implements DHTFuture<V> {

    private final Set<DHTFutureListener<V>> listeners 
        = new LinkedHashSet<DHTFutureListener<V>>();
    
    protected DHTFutureTask(Callable<V> callable) {
        super(callable);
    }
    
    /**
     * This method is called after a DHTFuture has finished its task.
     * You may override it to deregister a DHTFuture from a pool of
     * Futures for example
     */
    protected void deregister() {
        // DO NOTHING
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.concurrent.DHTFuture#addDHTFutureListener(com.limegroup.mojito.concurrent.DHTFutureListener)
     */
    public void addDHTFutureListener(DHTFutureListener<V> listener) {
        if (listener == null) {
            throw new NullPointerException("DHTFutureListener is null");
        }
        
        // TODO: Not sure if it's a good or bad idea to
        // call the listener on the caller Thread if the
        // DHTFuture is done
        boolean done = false;
        synchronized (listeners) {
            done = isDone();
            if (!done) {
                listeners.add(listener);
            }
        }
        
        if (done) {
            try {
                V value = get();
                listener.handleFutureSuccess(value);
            } catch (ExecutionException e) {
                listener.handleFutureFailure(e);
            } catch (CancellationException e) {
                listener.handleFutureCancelled(e);
            } catch (InterruptedException e) {
                listener.handleFutureInterrupted(e);
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see java.util.concurrent.FutureTask#done()
     */
    @Override
    protected void done() {
        assert (isDone());
        
        super.done();
        deregister();
        
        try {
            V value = get();
            fireFutureSuccess(value);
        } catch (ExecutionException e) {
            fireFutureFailure(e);
        } catch (CancellationException e) {
            fireFutureCancelled(e);
        } catch (InterruptedException e) {
            fireFutureInterrupted(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private DHTFutureListener<V>[] listeners() {
        synchronized (listeners) {
            return listeners.toArray(new DHTFutureListener[0]);
        }
    }
    
    /**
     * Fires a success event
     */
    protected void fireFutureSuccess(V value) {
        for (DHTFutureListener<V> l : listeners()) {
            l.handleFutureSuccess(value);
        }
    }
    
    /**
     * Fires an ExecutionException event
     */
    protected void fireFutureFailure(ExecutionException e) {
        for (DHTFutureListener<V> l : listeners()) {
            l.handleFutureFailure(e);
        }
    }
    
    /**
     * Fires an CancellationException event
     */
    protected void fireFutureCancelled(CancellationException e) {
        for (DHTFutureListener<V> l : listeners()) {
            l.handleFutureCancelled(e);
        }
    }
    
    /**
     * Fires an InterruptedException event
     */
    protected void fireFutureInterrupted(InterruptedException e) {
        for (DHTFutureListener<V> l : listeners()) {
            l.handleFutureInterrupted(e);
        }
    }
}
