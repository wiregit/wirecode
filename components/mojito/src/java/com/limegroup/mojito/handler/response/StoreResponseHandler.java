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

package com.limegroup.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.event.StoreEvent;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;
import com.limegroup.mojito.messages.StoreResponse.Status;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.util.ContactUtils;

/**
 * 
 */
public class StoreResponseHandler extends AbstractResponseHandler<StoreEvent> {

    private static final Log LOG = LogFactory.getLog(StoreResponseHandler.class);
    
    /** */
    private KUID valueId;
    
    /** */
    private Contact node;
    
    /** */
    private QueryKey queryKey;
    
    /** */
    private Collection<DHTValue> values;
    
    /** */
    private List<StoreState> storeStates = new ArrayList<StoreState>();
    
    /** */
    private Iterator<StoreState> states = null;
    
    /** */
    private Map<KUID, StoreState> activeStates = new HashMap<KUID, StoreState>();
    
    /** */
    private int parallelism = KademliaSettings.PARALLEL_STORES.getValue();
    
    public StoreResponseHandler(Context context, DHTValue value) {
        this(context, null, null, Arrays.asList(value));
    }
    
    @SuppressWarnings("unchecked")
    public StoreResponseHandler(Context context, 
            Collection<? extends DHTValue> values) {
        this(context, null, null, values);
    }

    public StoreResponseHandler(Context context, 
            Contact node, QueryKey queryKey, DHTValue value) {
        this(context, node, queryKey, Arrays.asList(value));
    }
    
    @SuppressWarnings("unchecked")
    public StoreResponseHandler(Context context, Contact node, 
            QueryKey queryKey, Collection<? extends DHTValue> values) {
        super(context);
        
        assert (values != null && !values.isEmpty());
        
        this.node = node;
        this.queryKey = queryKey;
        this.values = (Collection<DHTValue>)values;
        
        if (!isSingleNodeStore()) {
            for (DHTValue value : values) {
                if (valueId == null) {
                    valueId = value.getValueID();
                }
                
                if (!valueId.equals(value.getValueID())) {
                    throw new AssertionError("All DHTValues must have the same ID");
                }
            }
        }
    }

    /**
     * Returns the Collection of DHTValues
     */
    public Collection<DHTValue> getValues() {
        return values;
    }
    
    /**
     * Returns true if this handler is storing the DHTValues
     * at a single Node in the DHT
     */
    private boolean isSingleNodeStore() {
        return node != null;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected synchronized void start() throws Exception {

        if (isSingleNodeStore()) {
            if (queryKey == null) {
                GetQueryKeyHandler handler = new GetQueryKeyHandler(node);
                queryKey = handler.call();
            }
            
            if (queryKey == null) {
                throw new Exception("QueryKey is null");
            }
            
            storeStates.add(new StoreState(node, queryKey, values));
            
        } else {
            FindNodeResponseHandler handler 
                = new FindNodeResponseHandler(context, valueId);
            List<Entry<Contact,QueryKey>> nodes = handler.call().getNodes();
            
            for (Entry<Contact,QueryKey> entry : nodes) {
                Contact node = entry.getKey();
                QueryKey queryKey = entry.getValue();
                storeStates.add(new StoreState(node, queryKey, values));
            }
        }
        
        states = storeStates.iterator();
        sendNextAndExitIfDone();
    }
    
    @Override
    protected synchronized void response(ResponseMessage message, long time) throws IOException {
        StoreResponse response = (StoreResponse)message;
        Collection<Entry<KUID,Status>> status = response.getStatus();
        
        Contact node = message.getContact();
        KUID nodeId = node.getNodeID();
        
        if (activeStates.get(nodeId).response(status)) {
            activeStates.remove(nodeId);
        }
        
        sendNextAndExitIfDone();
    }

    @Override
    protected synchronized void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        
        if (activeStates.get(nodeId).timeout(nodeId, dst, message, time)) {
            activeStates.remove(nodeId);
        }
        
        sendNextAndExitIfDone();
    }
    
    @Override
    protected synchronized void error(KUID nodeId, SocketAddress dst, 
            RequestMessage message, Exception e) {
        
        StoreState state = activeStates.get(nodeId);
        if (state != null && state.error(e)) {
            activeStates.remove(nodeId);
        }
        
        sendNextAndExitIfDone();
    }

    /**
     * Tries to maintain parallel store requests and fires
     * an event if storing is done
     */
    private synchronized void sendNextAndExitIfDone() {
        while(activeStates.size() < parallelism && states.hasNext()) {
            StoreState state = states.next();
            
            try {
                if (state.start()) {
                    activeStates.put(state.node.getNodeID(), state);
                }
            } catch (IOException err) {
                state.exception = err;
                LOG.error("IOException", err);
            }
        }
        
        // No active states left? We're done!
        if (activeStates.isEmpty()) {
            done();
        }
    }
    
    /**
     * 
     */
    private synchronized void done() {
        List<Contact> nodes = new ArrayList<Contact>();
        Set<DHTValue> failed = new HashSet<DHTValue>();
        
        for (StoreState s : storeStates) {
            nodes.add(s.node);
            failed.addAll(s.getFailedValues());
        }
        
        if (storeStates.size() == 1) {
            StoreState s = storeStates.get(0);
            if (s.exception != null) {
                setException(s.exception);
            } else if (s.timeout >= 0L) {
                fireTimeoutException(s.nodeId, s.dst, s.message, s.timeout);
            }
        }
        
        setReturnValue(new StoreEvent(nodes, values, failed));
    }
    
    /**
     * 
     */
    private class StoreState {
        
        /* */
        private Contact node;
        private QueryKey queryKey;
        
        /* */
        private Iterator<DHTValue> it;
        
        /* */
        private DHTValue lastValue = null;
        
        /* */
        private List<DHTValue> failed = new ArrayList<DHTValue>();
        
        /* */
        private KUID nodeId;
        private SocketAddress dst;
        private RequestMessage message;
        private long timeout = -1L;
        
        /* */
        private Exception exception;
        
        @SuppressWarnings("unchecked")
        private StoreState(Contact node, QueryKey queryKey, 
                Collection<? extends DHTValue> values) {
            this.node = node;
            this.queryKey = queryKey;
            
            if (context.isLocalNode(node)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Skipping local Node");
                }
                
                this.it = new Iterator<DHTValue>() {
                    public boolean hasNext() {
                        return false;
                    }
                    
                    public DHTValue next() {
                        throw new NoSuchElementException();
                    }
                    
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            } else {
                this.it = (Iterator<DHTValue>)values.iterator();
            }
        }
        
        /**
         * Starts the store process at the given Node
         */
        @SuppressWarnings("unchecked")
        public boolean start() throws IOException {
            return !response(null);
        }
        
        /**
         * Handles a response and returns true if done
         */
        public boolean response(Collection<? extends Entry<KUID,Status>> status) throws IOException {
            if (lastValue != null) {
                if (status != null && status.size() == 1) {
                    Entry<KUID,Status> e = status.iterator().next();
                    if (e.getValue() != StoreResponse.Status.SUCCEEDED) {
                        failed.add(lastValue);
                    }
                } else {
                    failed.add(lastValue);
                }
                
                lastValue = null;
            }
            
            if (!it.hasNext()) {
                return true;
            }
            
            // TODO: http://en.wikipedia.org/wiki/Knapsack_problem
            
            lastValue = it.next();
            StoreRequest request = context.getMessageHelper()
                .createStoreRequest(node.getContactAddress(), queryKey, Arrays.asList(lastValue));
            context.getMessageDispatcher().send(node, request, StoreResponseHandler.this);

            return false;
        }

        /**
         * Handles a timeout and returns true if done
         */
        public boolean timeout(KUID nodeId, SocketAddress dst, 
                RequestMessage message, long timeout) throws IOException {
            this.nodeId = nodeId;
            this.dst = dst;
            this.message = message;
            this.timeout = timeout;
            return true;
        }
        
        /**
         * Handles an error and returns true if done
         */
        public boolean error(Exception e) {
            this.exception = e;
            return true;
        }
        
        /**
         * Returns a list of all DHTValues that couldn't be
         * stored at the given Node
         */
        public List<DHTValue> getFailedValues() {
            if (lastValue != null) {
                failed.add(lastValue);
                lastValue = null;
            }
            
            while(it.hasNext()) {
                failed.add(it.next());
            }
            
            return failed;
        }
    }
    
    /**
     * GetQueryKeyHandler tries to get the QueryKey of a Node
     */
    private class GetQueryKeyHandler extends AbstractResponseHandler<QueryKey> {
        
        private Contact node;
        
        private GetQueryKeyHandler(Contact node) {
            super(StoreResponseHandler.this.context);
            
            this.node = node;
        }

        @Override
        protected void start() throws Exception {
            RequestMessage request = context.getMessageHelper()
                .createFindNodeRequest(node.getContactAddress(), node.getNodeID());
            context.getMessageDispatcher().send(node, request, this);
        }

        protected void response(ResponseMessage message, long time) throws IOException {
            
            FindNodeResponse response = (FindNodeResponse)message;
            
            Collection<Contact> nodes = response.getNodes();
            for(Contact node : nodes) {
                
                if (!ContactUtils.isValidContact(response.getContact(), node)) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Dropping " + node);
                    }
                    continue;
                }
                
                if (ContactUtils.isLocalNode(context, node, null)) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Dropping " + node);
                    }
                    continue;
                }
                
                // We did a FIND_NODE lookup use the info
                // to fill/update our routing table
                assert (node.isAlive() == false);
                context.getRouteTable().add(node);
            }
            
            setReturnValue(response.getQueryKey());
        }

        protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
            fireTimeoutException(nodeId, dst, message, time);
        }

        public void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Getting the QueryKey from " + ContactUtils.toString(nodeId, dst) + " failed", e);
            }
            
            setException(e);
        }
    }
}
