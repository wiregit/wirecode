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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.exceptions.DHTBackendException;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;
import com.limegroup.mojito.messages.StoreResponse.Status;
import com.limegroup.mojito.result.StoreResult;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.util.ContactUtils;

/**
 * The StoreResponseHandler class handles/manages storing of
 * DHTValues on remote Nodes
 */
public class StoreResponseHandler extends AbstractResponseHandler<StoreResult> {

    private static final Log LOG = LogFactory.getLog(StoreResponseHandler.class);
    
    /** The ID of the Value */
    private KUID valueId;
    
    /** 
     * The remote Node wehere to store the Value(s). 
     * Can be null.
     */
    private Contact node;
    
    /** The QueryKey we have to use. Can be null. */
    private QueryKey queryKey;
    
    /** The Value(s) we're going to store */
    private Collection<DHTValue> values;
    
    /** A list of StoreProcesses. One StoreProcess per Contact */
    private List<StoreProcess> processList = new ArrayList<StoreProcess>();
    
    /** An Iterator of StoreProcesses (see processList) */
    private Iterator<StoreProcess> processes = null;
    
    /** Map of currently active StoreProcesses (see parallelism) */
    private Map<KUID, StoreProcess> activeProcesses = new HashMap<KUID, StoreProcess>();
    
    /** The number of parallel stores */
    private int parallelism = KademliaSettings.PARALLEL_STORES.getValue();
    
    public StoreResponseHandler(Context context, DHTValue value) {
        this(context, null, null, Collections.singletonList(value));
    }
    
    @SuppressWarnings("unchecked")
    public StoreResponseHandler(Context context, 
            Collection<? extends DHTValue> values) {
        this(context, null, null, values);
    }

    public StoreResponseHandler(Context context, 
            Contact node, QueryKey queryKey, DHTValue value) {
        this(context, node, queryKey, Collections.singletonList(value));
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
                    throw new IllegalArgumentException("All DHTValues must have the same Value ID");
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
    protected synchronized void start() throws DHTException {
        super.start();
        
        if (isSingleNodeStore()) {
            
            // Get the QueryKey if we don't have it
            if (queryKey == null) {
                GetQueryKeyHandler handler = new GetQueryKeyHandler(context, node);
                
                try {
                    queryKey = handler.call();
                } catch (InterruptedException e) {
                    throw new DHTException(e);
                }
            }
            
            if (queryKey == null) {
                throw new IllegalStateException("QueryKey is null");
            }
            
            processList.add(new StoreProcess(node, queryKey, values));
            
        } else {
            // Do a lookup for the k-closest Nodes where we're
            // going to store the value
            FindNodeResponseHandler handler 
                = new FindNodeResponseHandler(context, valueId);
            
            // Use only alive Contacts from the RouteTable
            handler.setSelectAliveNodesOnly(true);
            
            Map<Contact,QueryKey> nodes = null;
            try {
                nodes = handler.call().getNodes();
            } catch (InterruptedException e) {
                throw new DHTException(e);
            }
            
            for (Entry<Contact,QueryKey> entry : nodes.entrySet()) {
                Contact node = entry.getKey();
                QueryKey queryKey = entry.getValue();
                processList.add(new StoreProcess(node, queryKey, values));
            }
            
        }
        
        processes = processList.iterator();
        sendNextAndExitIfDone();
    }
    
    @Override
    protected synchronized void response(ResponseMessage message, long time) throws IOException {
        StoreResponse response = (StoreResponse)message;
        Collection<Entry<KUID,Status>> status = response.getStatus();
        
        Contact node = message.getContact();
        KUID nodeId = node.getNodeID();
        
        if (activeProcesses.get(nodeId).response(status)) {
            activeProcesses.remove(nodeId);
        }
        
        sendNextAndExitIfDone();
    }

    @Override
    protected synchronized void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        
        if (activeProcesses.get(nodeId).timeout(nodeId, dst, message, time)) {
            activeProcesses.remove(nodeId);
        }
        
        sendNextAndExitIfDone();
    }
    
    @Override
    protected synchronized void error(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e) {
        
        StoreProcess state = activeProcesses.get(nodeId);
        if (state != null && state.error(e)) {
            activeProcesses.remove(nodeId);
        }
        
        sendNextAndExitIfDone();
    }

    /**
     * Tries to maintain parallel store requests and fires
     * an event if storing is done
     */
    private synchronized void sendNextAndExitIfDone() {
        while(activeProcesses.size() < parallelism && processes.hasNext()) {
            StoreProcess process = processes.next();
            
            try {
                if (process.start()) {
                    Contact node = process.node;
                    activeProcesses.put(node.getNodeID(), process);
                }
            } catch (IOException err) {
                process.exception = err;
                LOG.error("IOException", err);
            }
        }
        
        // No active processes left? We're done!
        if (activeProcesses.isEmpty()) {
            done();
        }
    }
    
    /**
     * Called if all values were stored
     */
    private synchronized void done() {
        List<Contact> nodes = new ArrayList<Contact>();
        Set<DHTValue> failed = new HashSet<DHTValue>();
        
        for (StoreProcess s : processList) {
            nodes.add(s.node);
            failed.addAll(s.getFailedValues());
        }
        
        if (processList.size() == 1) {
            StoreProcess s = processList.get(0);
            if (s.exception != null) {
                setException(new DHTException(s.exception));
            } else if (s.timeout >= 0L) {
                fireTimeoutException(s.nodeId, s.dst, s.message, s.timeout);
            }
        }
        
        setReturnValue(new StoreResult(nodes, values, failed));
    }
    
    /**
     * The StoreProcess class manages storing of values on a single Node
     */
    private class StoreProcess {
        
        /** The Node to where to store the values */
        private Contact node;
        
        /** The QueryKey for the Node */
        private QueryKey queryKey;
        
        /** The Values to store */
        private Iterator<DHTValue> it;
        
        /** The value that is currently beeing stored */
        private DHTValue value = null;
        
        /** A List of values that couldn't be stored */
        private List<DHTValue> failed = new ArrayList<DHTValue>();
        
        /*  */
        private KUID nodeId;
        private SocketAddress dst;
        private RequestMessage message;
        private long timeout = -1L;
        
        /** A reference to an Exception that iterrupted this store process */
        private Exception exception;
        
        @SuppressWarnings("unchecked")
        private StoreProcess(Contact node, QueryKey queryKey, 
                Collection<? extends DHTValue> values) {
            this.node = node;
            this.queryKey = queryKey;
            
            if (context.isLocalNode(node)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Skipping local Node");
                }
                
                values = Collections.emptyList();
            }
            
            this.it = (Iterator<DHTValue>)values.iterator();
        }
        
        /**
         * Starts the store process at the given Node
         */
        @SuppressWarnings("unchecked")
        public boolean start() throws IOException {
            // We start by saying we've received a fictive response
            return !response(null);
        }
        
        /**
         * Handles a response and returns true if done
         */
        public boolean response(Collection<? extends Entry<KUID, Status>> status) throws IOException {
            // value is null on this first iteration
            if (value != null) {
                
                // We store one value per request (scroll down).
                if (status != null && status.size() == 1) {
                    Entry<KUID,Status> e = status.iterator().next();
                    if (!Status.SUCCEEDED.equals(e.getValue())) {
                        failed.add(value);
                    }
                } else {
                    failed.add(value);
                }
                
                value = null;
            }
            
            // We're finished if nothing left to store
            if (!it.hasNext()) {
                return true;
            }
            
            // Get the next value and store it at the remote Node
            // TODO: http://en.wikipedia.org/wiki/Knapsack_problem
            
            value = it.next();
            StoreRequest request = context.getMessageHelper()
                .createStoreRequest(node.getContactAddress(), queryKey, Collections.singleton(value));
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
            if (value != null) {
                failed.add(value);
                value = null;
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
    private static class GetQueryKeyHandler extends AbstractResponseHandler<QueryKey> {
        
        private Contact node;
        
        GetQueryKeyHandler(Context context, Contact node) {
            super(context);
            
            this.node = node;
        }

        @Override
        protected void start() throws DHTException {
            RequestMessage request = context.getMessageHelper()
                .createFindNodeRequest(node.getContactAddress(), node.getNodeID());
            
            try {
                context.getMessageDispatcher().send(node, request, this);
            } catch (IOException err) {
                throw new DHTException(err);
            }
        }
        
        @Override
        protected void response(ResponseMessage message, long time) throws IOException {
            
            FindNodeResponse response = (FindNodeResponse)message;
            
            Collection<Contact> nodes = response.getNodes();
            for(Contact node : nodes) {
                
                if (!ContactUtils.isValidContact(response.getContact(), node)) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Dropping invalid Contact " + node);
                    }
                    continue;
                }
                
                // Make sure we're not mixing IPv4 and IPv6 addresses.
                // See RouteTableImpl.add() for more Info!
                if (!ContactUtils.isSameAddressSpace(context.getLocalNode(), node)) {
                    
                    // Log as ERROR so that we're not missing this
                    if (LOG.isErrorEnabled()) {
                        LOG.error(node + " is from a different IP address space than local Node");
                    }
                    continue;
                }
                
                if (ContactUtils.isLocalContact(context, node, null)) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Dropping local Contact " + node);
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
        
        @Override
        protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
            fireTimeoutException(nodeId, dst, message, time);
        }

        @Override
        protected void error(KUID nodeId, SocketAddress dst, RequestMessage message, IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Getting the QueryKey from " + ContactUtils.toString(nodeId, dst) + " failed", e);
            }
            
            setException(new DHTBackendException(nodeId, dst, message, e));
        }
    }
}
