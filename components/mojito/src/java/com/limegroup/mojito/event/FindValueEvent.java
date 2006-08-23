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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;

/**
 * 
 */
public class FindValueEvent implements Iterable<DHTValue> {
    
    private static final Log LOG = LogFactory.getLog(FindValueEvent.class);
    
    private Context context;
    
    private KUID lookupId;
    
    private List<FindValueResponse> responses;
    
    private long time;
    
    private int hop;
    
    @SuppressWarnings("unchecked")
    public FindValueEvent(Context context, KUID lookupId, 
            List<? extends FindValueResponse> values, long time, int hop) {
        
        this.context = context;
        this.lookupId = lookupId;
        this.responses = (List<FindValueResponse>)values;
        this.time = time;
        this.hop = hop;
    }
    
    public KUID getLookupID() {
        return lookupId;
    }
    
    public Iterator<DHTValue> iterator() {
        return new ResponseIterator();
    }
    
    public long getTime() {
        return time;
    }
    
    public int getHop() {
        return hop;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(lookupId).append(" (time=").append(time).append("ms, hop=").append(hop).append(")\n");
        int i = 0;
        for (DHTValue value : this) {
            buffer.append(i++).append(": ").append(value).append("\n");
        }
        return buffer.toString();
    }
    
    private class ResponseIterator implements Iterator<DHTValue> {
        
        private Iterator<FindValueResponse> it = responses.iterator();
        
        private Iterator<DHTValue> values = null;
        
        public boolean hasNext() {
            return it.hasNext() || (values != null && values.hasNext());
        }

        public DHTValue next() {
            if (values == null || !values.hasNext()) {
                if (it.hasNext()) {
                    values = new GetValueIterator(it.next());
                }
            }
            
            if (values != null && values.hasNext()) {
                return values.next();
            }
            
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
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
                    DHTValue v = next(key);
                    return v;
                } catch (Exception err) {
                    LOG.error("Exception", err);
                    if (keys.hasNext()) {
                        return next();
                    }

                    throw new NoSuchElementException(err.getMessage());
                }
            }
            
            throw new NoSuchElementException();
        }

        private DHTValue next(KUID nodeId) throws Exception {
            GetValueResponseHandler getValues = new GetValueResponseHandler(node, lookupId, nodeId);
            Collection<DHTValue> v = getValues.call();
            values = v.iterator();
            return values.next();
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
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
            
            List<KUID> nodeIds = Arrays.asList(nodeId);
            FindValueRequest request = context.getMessageHelper()
                .createFindValueRequest(node.getContactAddress(), valueId, nodeIds);
            
            context.getMessageDispatcher().send(node, request, this);
        }

        @Override
        protected void response(ResponseMessage message, long time) throws IOException {
            Collection<DHTValue> values = ((FindValueResponse)message).getValues();
            setReturnValue(values);
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
