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

package com.limegroup.mojito.event;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;

/**
 * The FindNodeEvent is fired when a FIND_VALUE lookup
 * finishes
 */
public class FindValueEvent {
    
    private static final Log LOG = LogFactory.getLog(FindValueEvent.class);
    
    private Context context;
    
    private KUID lookupId;
    
    private List<FindValueResponse> responses;
    
    private long time;
    
    private int hop;
    
    @SuppressWarnings("unchecked")
    public FindValueEvent(Context context, 
    		KUID lookupId, 
            List<? extends FindValueResponse> values, 
            long time, int hop) {
        
        this.context = context;
        this.lookupId = lookupId;
        this.responses = (List<FindValueResponse>)values;
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
     * Returns a Callable that works like an Iterator. The call()
     * Method returns null if all DHTValues were retireved from
     * the remote Node(s).
     */
    public Callable<DHTValue> getCallable() {
        return new GetValueCallable();
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
        try {
            int i = 0;
            DHTValue value = null;
            Callable<DHTValue> c = getCallable();
            while((value = c.call()) != null) {
                buffer.append(i++).append(": ").append(value).append("\n");
            }
        } catch (Exception err) {
            buffer.append(err.toString());
        }
        return buffer.toString();
    }
    
    /**
     * The GetValueCallable class iterates through all FindValueResponses
     * and tries to get the DHTValues from each Node
     */
    private class GetValueCallable implements Callable<DHTValue> {
        
        private Iterator<FindValueResponse> resps = responses.iterator();
        
        @SuppressWarnings("unchecked")
        private Iterator<DHTValue> values = Collections.EMPTY_LIST.iterator();
        
        public DHTValue call() throws Exception {
            while(resps.hasNext() || values.hasNext()) {
                
                if (!values.hasNext()) {
                    if (!resps.hasNext()) {
                        // EOF
                        break;
                    }
                    
                    values = new GetValueIterator(resps.next());
                }
                
                try {
                    return values.next();
                } catch (NoSuchElementException err) {
                    LOG.info("NoSuchElementException", err);
                    // Continue with next DHTValue or FindValueResponse!
                }
            }
            
            return null;
        }
    }

    /**
     * The GetValueIterator iterates through all DHTValues on
     * a remote Node and tries to get them
     */
    private class GetValueIterator implements Iterator<DHTValue> {
        
        private Contact node;
        
        private Iterator<KUID> keys;
        
        private Iterator<DHTValue> values;
        
        private GetValueIterator(FindValueResponse response) {
            this.node = response.getContact();
            this.keys = response.getKeys().iterator();
            this.values = response.getValues().iterator();
        }

        public boolean hasNext() {
            return keys.hasNext() || values.hasNext();
        }

        public DHTValue next() {
            if (values.hasNext()) {
                return values.next();
            }
            
            if (keys.hasNext()) {
                try {
                    KUID key = keys.next();
                    initNext(key);
                    return next();
                } catch (Exception err) {
                    LOG.error("Exception", err);
                    
                    // If there are keys left then continue 
                    // with the next key
                    if (keys.hasNext()) {
                        return next();
                    }

                    throw new NoSuchElementException(err.getMessage());
                }
            }
            
            throw new NoSuchElementException();
        }

        private void initNext(KUID nodeId) throws Exception {
            GetValueResponseHandler getValues = new GetValueResponseHandler(node, lookupId, nodeId);
            values = getValues.call().iterator();
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * The GetValueResponseHandler retrieves DHTValues from 
     * a remote Node
     */
    private class GetValueResponseHandler extends AbstractResponseHandler<Collection<DHTValue>> {
        
        private Contact node;
        
        private KUID valueId;
        
        private KUID nodeId;
        
        private GetValueResponseHandler(Contact node, KUID valueId, KUID nodeId) {
            super(FindValueEvent.this.context);
            
            this.node = node;
            this.valueId = valueId;
            this.nodeId = nodeId;
        }
        
        @Override
        protected void start() throws Exception {
            super.start();
            
            List<KUID> nodeIds = Collections.singletonList(nodeId);
            FindValueRequest request = context.getMessageHelper()
                .createFindValueRequest(node.getContactAddress(), valueId, nodeIds);
            
            context.getMessageDispatcher().send(node, request, this);
        }

        @Override
        protected void response(ResponseMessage message, long time) throws IOException {
            if (message instanceof FindValueResponse) {
                Collection<DHTValue> values = ((FindValueResponse)message).getValues();
                setReturnValue(values);
                
            // Imagine the following case: We do a lookup for a value 
            // on the 59th minute and start retrieving the values on 
            // the 60th minute. As values expire on the 60th minute 
            // it may no longer exists and the remote Node returns us
            // a Set of the k-closest Nodes instead.
            } else if (message instanceof FindNodeResponse) {
                Collection<DHTValue> values = Collections.emptyList();
                setReturnValue(values);
            } else {
                setException(new IllegalArgumentException(message.toString()));
            }
        }
        
        @Override
        protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
            fireTimeoutException(nodeId, dst, message, time);
        }
        
        @Override
        protected void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
            setException(new DHTException(nodeId, dst, message, -1L, e));
        }
    }
}
