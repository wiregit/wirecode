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

package com.limegroup.mojito.result;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.routing.Contact;

/**
 * The FindValueResult is fired when a FIND_VALUE lookup finishes
 */
public class FindValueResult implements Iterable<Future<DHTValue>> {
    
    private static final Log LOG = LogFactory.getLog(FindValueResult.class);
    
    private Context context;
    
    private KUID lookupId;
    
    private Collection<FindValueResponse> responses;
    
    private long time;
    
    private int hop;
    
    @SuppressWarnings("unchecked")
    public FindValueResult(Context context, 
    		KUID lookupId, 
                Collection<? extends FindValueResponse> values, 
            long time, int hop) {
        
        this.context = context;
        this.lookupId = lookupId;
        this.responses = (Collection<FindValueResponse>)values;
        this.time = time;
        this.hop = hop;
    }
    
    /**
     * Returns the KUID we were looking for
     */
    public KUID getLookupID() {
        return lookupId;
    }
    
    /**
     * Returns an Iterator of Futures that return the DHTValue(s)
     */
    public Iterator<Future<DHTValue>> iterator() {
        return new ValuesIterator();
    }

    /**
     * Returns the amount of time it took to find the DHTValue(s)
     */
    public long getTime() {
        return time;
    }
    
    /**
     * Returns the number of hops it took to find the DHTValue(s)
     */
    public int getHop() {
        return hop;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(lookupId).append(" (time=").append(time)
            .append("ms, hop=").append(hop).append(")\n");

        if(responses.isEmpty()) {
            buffer.append("No values found!");
            return buffer.toString();
        }
        
        int i = 0;
        for (Future<DHTValue> future : this) {
            try {
                DHTValue value = future.get();
                buffer.append(i++).append(": ").append(value).append("\n");
            } catch (InterruptedException err) {
                LOG.error("InterruptedException", err);
                buffer.append(err);
            } catch (ExecutionException err) {
                LOG.error("ExecutionException", err);
                buffer.append(err);
            }
        }
        
        for(FindValueResponse resp: responses) {
            buffer.append("Response from: ").append(resp.getContact().toString())
                .append("\n").append("Load: ").append(resp.getRequestLoad()).append("\n");
        }
        
        return buffer.toString();
    }
    
    /**
     * The ValuesIterator class iterates through all FindValueResponses
     * we received (one response per Node and if the lookup wasn't 
     * exhaustive there's only one FindValueResponse) and delegates 
     * all method calls to ContactValuesIterator.
     */
    private class ValuesIterator implements Iterator<Future<DHTValue>> {
        
        private Iterator<FindValueResponse> resps = responses.iterator();
        
        @SuppressWarnings("unchecked")
        private Iterator<Future<DHTValue>> values = Collections.EMPTY_LIST.iterator();
        
        public boolean hasNext() {
            return resps.hasNext() || values.hasNext();
        }
        
        public Future<DHTValue> next() {
            if (!values.hasNext()) {
                if (!resps.hasNext()) {
                    throw new NoSuchElementException();
                }
                
                values = new ContactValuesIterator(resps.next());
            }
            
            return values.next();
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * The ContactValuesIterator class iterates through all values
     * the remote Node send us first and continues with retreiving
     * values from the remote Node.
     */
    private class ContactValuesIterator implements Iterator<Future<DHTValue>> {
        
        private Contact node;
        
        private Iterator<KUID> keys;
        
        private Iterator<DHTValue> values;
        
        private ContactValuesIterator(FindValueResponse response) {
            this.node = response.getContact();
            this.keys = response.getKeys().iterator();
            this.values = response.getValues().iterator();
        }

        public boolean hasNext() {
            return keys.hasNext() || values.hasNext();
        }

        public Future<DHTValue> next() {
            // Return the values we aleady have first...
            if (values.hasNext()) {
                return new ReturnDHTValueFuture(values.next());
            }
            
            // ...and continue with retreiving the values
            // from the remote Node
            if (keys.hasNext()) {
                KUID nodeId = keys.next();
                Future<Collection<DHTValue>> future = context.get(node, lookupId, nodeId);
                return new GetDHTValueFuture(future);
            }
            
            throw new NoSuchElementException();
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Wraps a single DHTValue into a Future
     */
    private static class ReturnDHTValueFuture implements Future<DHTValue> {
        
        private DHTValue value;
        
        private ReturnDHTValueFuture(DHTValue value) {
            this.value = value;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        public DHTValue get() throws InterruptedException, ExecutionException {
            return value;
        }

        public DHTValue get(long timeout, TimeUnit unit) 
                throws InterruptedException, ExecutionException, TimeoutException {
            return value;
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return true;
        }
    }
    
    /**
     * Wraps a Future as returned by GetValueManager and the
     * get methods return a single value instead of a Collection
     * of values.
     * 
     * We count on the fact that we're requesting a single value
     * and receive either none or one value.
     */
    private static class GetDHTValueFuture implements Future<DHTValue> {
        
        private Future<Collection<DHTValue>> future;
        
        private GetDHTValueFuture(Future<Collection<DHTValue>> future) {
            this.future = future;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        public DHTValue get() throws InterruptedException, ExecutionException {
            Collection<DHTValue> values = future.get();
            if (values.size() > 1) {
                throw new IllegalStateException("Expected none or one DHTValue: " + values);
            }
            
            // Can fail with NoSuchElementException which is OK
            return values.iterator().next();
        }

        public DHTValue get(long timeout, TimeUnit unit) 
                throws InterruptedException, ExecutionException, TimeoutException {
            Collection<DHTValue> values = future.get(timeout, unit);
            if (values.size() > 1) {
                throw new IllegalStateException("Expected none or one DHTValue: " + values);
            }
            
            // Can fail with NoSuchElementException which is OK
            return values.iterator().next();
        }

        public boolean isCancelled() {
            return future.isCancelled();
        }

        public boolean isDone() {
            return future.isDone();
        }
    }
}
