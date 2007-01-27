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

package org.limewire.mojito.result;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.FixedDHTFuture;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;


/**
 * The FindValueResult is fired when a FIND_VALUE lookup finishes
 */
public class FindValueResult extends LookupResult implements Iterable<Future<DHTValueEntity>> {
    
    private static final Log LOG = LogFactory.getLog(FindValueResult.class);
    
    private final Context context;
    
    private final Map<? extends Contact, ? extends SecurityToken> path;
    
    private final Collection<? extends FindValueResponse> responses;
    
    private final long time;
    
    private final int hop;
    
    public FindValueResult(Context context, 
    		KUID lookupId, 
            Map<? extends Contact, ? extends SecurityToken> path,
            Collection<? extends FindValueResponse> responses, 
            long time, int hop) {
        super(lookupId);
        
        this.context = context;
        this.path = path;
        this.responses = responses;
        this.time = time;
        this.hop = hop;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.result.LookupPath#getPath()
     */
    public Collection<? extends Contact> getPath() {
        return path.keySet();
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.result.LookupPath#getSecurityToken(org.limewire.mojito.routing.Contact)
     */
    public SecurityToken getSecurityToken(Contact node) {
        return path.get(node);
    }
    
    /**
     * Returns an Iterator of Futures that return the DHTValue(s)
     */
    public Iterator<Future<DHTValueEntity>> iterator() {
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
        buffer.append(getLookupID()).append(" (time=").append(time)
            .append("ms, hop=").append(hop).append(")\n");

        if(responses.isEmpty()) {
            buffer.append("No values found!");
            return buffer.toString();
        }
        
        int i = 0;
        for (Future<DHTValueEntity> future : this) {
            try {
                DHTValueEntity entity = future.get();
                buffer.append(i++).append(": ").append(entity).append("\n");
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
    private class ValuesIterator implements Iterator<Future<DHTValueEntity>> {
        
        private Iterator<? extends FindValueResponse> resps = responses.iterator();
        
        private Iterator<Future<DHTValueEntity>> values;
        
        public ValuesIterator() {
            Iterable<Future<DHTValueEntity>> it = Collections.emptyList();
            values = it.iterator();
        }
        
        public boolean hasNext() {
            return resps.hasNext() || values.hasNext();
        }
        
        public Future<DHTValueEntity> next() {
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
    private class ContactValuesIterator implements Iterator<Future<DHTValueEntity>> {
        
        private Contact node;
        
        private Iterator<KUID> keys;
        
        private Iterator<? extends DHTValueEntity> values;
        
        private ContactValuesIterator(FindValueResponse response) {
            this.node = response.getContact();
            this.keys = response.getKeys().iterator();
            this.values = response.getValues().iterator();
        }

        public boolean hasNext() {
            return keys.hasNext() || values.hasNext();
        }

        public Future<DHTValueEntity> next() {
            // Return the values we aleady have first...
            if (values.hasNext()) {
                return new FixedDHTFuture<DHTValueEntity>(values.next());
            }
            
            // ...and continue with retreiving the values
            // from the remote Node
            if (keys.hasNext()) {
                KUID nodeId = keys.next();
                
                DHTFuture<GetValueResult> future 
                    = context.get(node, getLookupID(), nodeId);
                
                return new GetDHTValueFuture(future);
            }
            
            throw new NoSuchElementException();
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
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
    private static class GetDHTValueFuture implements Future<DHTValueEntity> {
        
        private Future<GetValueResult> future;
        
        private GetDHTValueFuture(Future<GetValueResult> future) {
            this.future = future;
        }
        
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        public DHTValueEntity get() throws InterruptedException, ExecutionException {
            Collection<? extends DHTValueEntity> values = future.get().getValues();
            if (values.size() > 1) {
                throw new IllegalStateException("Expected none or one DHTValue: " + values);
            }
            
            // Can fail with NoSuchElementException which is OK
            return values.iterator().next();
        }

        public DHTValueEntity get(long timeout, TimeUnit unit) 
                throws InterruptedException, ExecutionException, TimeoutException {
            Collection<? extends DHTValueEntity> values = future.get(timeout, unit).getValues();
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
