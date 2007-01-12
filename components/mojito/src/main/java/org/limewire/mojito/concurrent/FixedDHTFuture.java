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

package org.limewire.mojito.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.mojito.util.OnewayExchanger;


/**
 * A FixedDHTFuture is hard coded to return a value or
 * throw an Exception.
 */
public class FixedDHTFuture<T> implements DHTFuture<T> {
    
    private OnewayExchanger<T, ExecutionException> exchanger 
        = new OnewayExchanger<T, ExecutionException>();
    
    public FixedDHTFuture(T value) {
        exchanger.setValue(value);
    }

    public FixedDHTFuture(Exception exception) {
        exchanger.setException(new ExecutionException(exception));
    }
    
    public void addDHTFutureListener(DHTFutureListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("DHTFutureListener is null");
        }
        
        try {
            T value = get();
            listener.handleFutureSuccess(value);
        } catch (ExecutionException e) {
            listener.handleFutureFailure(e);
        } catch (CancellationException e) {
            listener.handleFutureCancelled(e);
        } catch (InterruptedException e) {
            listener.handleFutureInterrupted(e);
        }
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return exchanger.cancel();
    }

    public T get() throws InterruptedException, ExecutionException {
        return exchanger.get();
    }

    public T get(long timeout, TimeUnit unit) 
            throws InterruptedException, ExecutionException, TimeoutException {
        return exchanger.get(timeout, unit);
    }

    public boolean isCancelled() {
        return exchanger.isCancelled();
    }

    public boolean isDone() {
        return exchanger.isDone();
    }
}
