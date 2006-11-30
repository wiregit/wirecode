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
import java.util.concurrent.FutureTask;


/**
 * An abstract implementation of DHTFuture
 */
public abstract class AbstractDHTFuture<V> extends FutureTask<V> implements DHTFuture<V> {

    private List<DHTFutureListener<V>> listeners 
        = new ArrayList<DHTFutureListener<V>>();
    
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
     * @see com.limegroup.mojito.concurrent.DHTFuture#addDHTFutureListener(com.limegroup.mojito.concurrent.DHTFutureListener)
     */
    public void addDHTFutureListener(DHTFutureListener<V> listener) {
        if (listener == null) {
            return;
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
            listener.futureDone(this);
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
        fireFutureDone();
    }
    
    public void fireFutureDone() {
        fireFutureDone(this);
    }
    
    public void fireFutureDone(DHTFuture<V> future) {
        List<DHTFutureListener<V>> copy = null;
        synchronized (listeners) {
            copy = new ArrayList<DHTFutureListener<V>>(listeners);
        }
        
        for (DHTFutureListener<V> l : copy) {
            l.futureDone(future);
        }
    }
}
