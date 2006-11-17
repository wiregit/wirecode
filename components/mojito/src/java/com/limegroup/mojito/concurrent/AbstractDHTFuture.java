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

package com.limegroup.mojito.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.limegroup.mojito.result.DHTResultListener;

/**
 * An abstract implementation of DHTFuture
 */
public abstract class AbstractDHTFuture<V> extends FutureTask<V> implements DHTFuture<V> {

    private List<DHTResultListener<V>> listeners 
        = new ArrayList<DHTResultListener<V>>();

    public AbstractDHTFuture(Callable<V> callable) {
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
     * @see com.limegroup.mojito.concurrent.DHTFuture#addDHTResultListener(com.limegroup.mojito.event.DHTEventListener)
     */
    public void addDHTResultListener(DHTResultListener<V> listener) {
        if (listener == null) {
            return;
        }
        
        if (isCancelled()) {
            return;
        }
        
        // TODO: Not sure if it's a good or bad idea to
        // call the listener on the caller Thread if the
        // DHTFuture is done
        synchronized (listeners) {
            if (isDone()) {
                try {
                    V result = get();
                    listener.handleResult(result);
                } catch (InterruptedException ignore) {
                } catch (ExecutionException ex) {
                    // NOTE: This is different from Future.cancel()!
                    // It's possible to cancel a ResponseHandler which
                    // is also throwing a CancellationException!
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
    
    /*
     * (non-Javadoc)
     * @see java.util.concurrent.FutureTask#done()
     */
    @Override
    protected void done() {
        super.done();
        
        deregister();
        
        if (!isCancelled()) {
            try {
                V result = get();
                fireResult(result);
            } catch (InterruptedException ignore) {
            } catch (ExecutionException ex) {
                // NOTE: This is different from Future.cancel()!
                // It's possible to cancel a ResponseHandler which
                // is also throwing a CancellationException!
                Throwable cause = ex.getCause();
                if (!(cause instanceof CancellationException)) {
                    fireThrowable(cause);
                }
            }
        }
    }
    
    /**
     * Notifies all listeners about the result. Meant to
     * be called on the DHTFuture Thread!
     */
    public void fireResult(V result) {
        synchronized(listeners) {
            for (DHTResultListener<V> l : listeners) {
                l.handleResult(result);
            }
        }
    }
    
    /**
     * Notifies all listeners about an Exception that occured. 
     * Meant to be called on the DHTFuture Thread!
     */
    public void fireThrowable(Throwable ex) {
        synchronized(listeners) {
            for (DHTResultListener<V> l : listeners) {
                l.handleThrowable(ex);
            }
        }
    }
}
