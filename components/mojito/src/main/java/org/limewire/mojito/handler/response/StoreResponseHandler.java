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

package org.limewire.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.messages.StoreRequest;
import org.limewire.mojito.messages.StoreResponse;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.StoreSettings;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.security.SecurityToken;

/**
 * The StoreResponseHandler class handles/manages storing of
 * DHTValues on remote Nodes
 */
public class StoreResponseHandler extends AbstractResponseHandler<StoreResult> {
    
    private static final Log LOG = LogFactory.getLog(StoreResponseHandler.class);
    
    private final Collection<? extends DHTValueEntity> entities;
    
    private final List<StoreProcess> processList = new ArrayList<StoreProcess>();
    
    /** An Iterator of StoreProcesses (see processList) */
    private Iterator<StoreProcess> processes = null;
    
    /** Map of currently active StoreProcesses (see parallelism) */
    private Map<KUID, StoreProcess> activeProcesses = new HashMap<KUID, StoreProcess>();
    
    /** The number of parallel stores */
    private final int parallelism = StoreSettings.PARALLEL_STORES.getValue();
    
    public StoreResponseHandler(Context context, 
            Collection<? extends Entry<? extends Contact, ? extends SecurityToken>> path, 
                    Collection<? extends DHTValueEntity> entities) {
        super(context);
        
        this.entities = entities;
        
        if (path.size() > KademliaSettings.REPLICATION_PARAMETER.getValue()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Path is longer than K: " + path.size() 
                        + " > " + KademliaSettings.REPLICATION_PARAMETER.getValue());
            }
        }
        
        // The order of the processes is as follows. The idea
        // is to not issue parallel store requests at a single Node.
        //
        // In other words we want this:
        // Value1(Node1), Value1(Node2), Value1(Node3)...
        // Value2(Node1), Value2(Node2), Value2(Node3)...
        // 
        // And NOT this:
        // Value1(Node1), Value2(Node1), Value3(Node1)...
        // Value1(Node2), Value2(Node2), Value3(Node2)...
        for (DHTValueEntity entity : entities) {
            for (Entry<? extends Contact, ? extends SecurityToken> entry : path) {
                Contact node = entry.getKey();
                SecurityToken securityToken = entry.getValue();
                
                if (context.isLocalNode(node)) {
                    processList.add(new LocalStoreProcess(node, entity));
                } else {
                    processList.add(new RemoteStoreProcess(node, securityToken, entity));
                }
            }
        }
    }
    
    @Override
    public void start() throws DHTException {
        processes = processList.iterator();
        sendNextAndExitIfDone();
    }
    
    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        StoreResponse response = (StoreResponse)message;
        Collection<StoreStatusCode> statusCodes = response.getStoreStatusCodes();
        
        Contact node = message.getContact();
        KUID nodeId = node.getNodeID();
        
        StoreProcess process = activeProcesses.remove(nodeId);
        if (process != null) {
            process.response(statusCodes);
        }
        
        sendNextAndExitIfDone();
    }
    
    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        
        StoreProcess process = activeProcesses.remove(nodeId);
        if (process != null) {
            process.setTimeout(time);
        }
        
        sendNextAndExitIfDone();
    }
    
    @Override
    protected void error(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e) {
        
        StoreProcess process = activeProcesses.remove(nodeId);
        if (process != null) {
            process.setException(e);
        }
        
        sendNextAndExitIfDone();
    }

    /**
     * Tries to maintain parallel store requests and fires
     * an event if storing is done
     */
    private void sendNextAndExitIfDone() {
        while(activeProcesses.size() < parallelism && processes.hasNext()) {
            StoreProcess process = processes.next();
            
            try {
                if (process.store()) {
                    Contact node = process.getContact();
                    activeProcesses.put(node.getNodeID(), process);
                }
            } catch (IOException err) {
                process.setException(err);
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
    private void done() {
        
        Map<Contact, Collection<StoreStatusCode>> map 
            = new LinkedHashMap<Contact, Collection<StoreStatusCode>>();
        
        Map<KUID, Collection<Contact>> locations 
            = new HashMap<KUID, Collection<Contact>>();
        
        for (StoreProcess process : processList) {
            Contact node = process.getContact();
            Collection<StoreStatusCode> statusCodes 
                = process.getStoreStatusCodes();
            map.put(node, statusCodes);
            
            for (StoreStatusCode statusCode : statusCodes) {
                if (!statusCode.getStatusCode().equals(StoreResponse.OK)) {
                    continue;
                }
                
                KUID secondaryKey = statusCode.getSecondaryKey();
                Collection<Contact> nodes = locations.get(secondaryKey);
                if (nodes == null) {
                    nodes = new ArrayList<Contact>();
                    locations.put(secondaryKey, nodes);
                }
                
                nodes.add(node);
            }
        }
        
        if (processList.size() == 1) {
            StoreProcess process = processList.get(0);
            if (process.getException() != null) {
                setException(new DHTException(process.getException()));
                return;
            } else if (process.getTimeout() != -1L) {
                Contact node = process.getContact();
                KUID nodeId = node.getNodeID();
                SocketAddress dst = node.getContactAddress();
                fireTimeoutException(nodeId, dst, null, process.getTimeout());
                return;
            }
        }
        
        StoreResult result = new StoreResult(map, entities);
        /*for (DHTValueEntity entity : entities) {
            entity.handleStoreResult(result);
        }*/
        
        setReturnValue(result);
    }
    
    /**
     * A StoreProcess manages 
     */
    private static interface StoreProcess {
        
        /**
         * Returns the Node where we are storing the values
         */
        public Contact getContact();
        
        /**
         * Returns the value this process is storing
         */
        public DHTValueEntity getDHTValueEntity();
        
        /**
         * Starts the process and returns true if the process
         * was started
         */
        public boolean store() throws IOException;
        
        /**
         * Called every time for every StoreResponse. Return
         * true if this StoreProcess is done with storing.
         */
        public void response(Collection<StoreStatusCode> statusCodes) throws IOException;
        
        /**
         * This returns essentially the result of the StoreProcess
         */
        public Collection<StoreStatusCode> getStoreStatusCodes();
        
        /**
         * Setter for Exception
         */
        public void setException(Exception exception);
        
        /**
         * Returns an Exception if one occured
         */
        public Exception getException();
        
        /**
         * Setter for timeout
         */
        public void setTimeout(long time);
        
        /**
         * Returns a non negative value if a timeout occured
         */
        public long getTimeout();
    }
    
    /**
     * LocalStoreProcess stores values in the local Node's Database
     */
    private class LocalStoreProcess implements StoreProcess {
        
        private final Contact node;
        
        private final DHTValueEntity entity;
        
        private final Collection<StoreStatusCode> statusCodes 
            = new ArrayList<StoreStatusCode>();
        
        public LocalStoreProcess(Contact node, DHTValueEntity entity) {
            assert (context.isLocalNode(node));
            this.node = node;
            this.entity = entity;
        }
        
        public Contact getContact() {
            return node;
        }
        
        public DHTValueEntity getDHTValueEntity() {
            return entity;
        }
        
        public boolean store() throws IOException {
            
            // Store the values in the local Database
            Database database = context.getDatabase();
            if (database.store(entity)) {
                statusCodes.add(new StoreStatusCode(entity, StoreResponse.OK));
            } else {
                statusCodes.add(new StoreStatusCode(entity, StoreResponse.ERROR));
            }
            
            // And exit here (do as if we were not able
            // to start this process)!
            return false;
        }
        
        public void response(Collection<StoreStatusCode> statusCodes) throws IOException {
            throw new UnsupportedOperationException();
        }
        
        public void setException(Exception exception) {
            throw new UnsupportedOperationException();
        }
        
        public Exception getException() {
            return null;
        }
        
        public void setTimeout(long time) {
            throw new UnsupportedOperationException();
        }
        
        public long getTimeout() {
            return -1L;
        }
        
        public Collection<StoreStatusCode> getStoreStatusCodes() {
            return statusCodes;
        }
    }
    
    /**
     * RemoteStoreProcess stores values at a remote Node
     */
    private class RemoteStoreProcess implements StoreProcess {
        
        /** The Node to where to store the values */
        private final Contact node;
        
        /** The SecurityToken for the Node */
        private final SecurityToken securityToken;
        
        /** The value that is currently beeing stored */
        private final DHTValueEntity entity;
        
        private Collection<StoreStatusCode> statusCodes = null;
        
        private long timeout = -1L;
        
        /** A reference to an Exception that iterrupted this store process */
        private Exception exception;
        
        private RemoteStoreProcess(Contact node, SecurityToken securityToken, 
                DHTValueEntity entity) {
            assert (!context.isLocalNode(node));
            this.node = node;
            this.securityToken = securityToken;
            this.entity = entity;
        }
        
        public Contact getContact() {
            return node;
        }
        
        public DHTValueEntity getDHTValueEntity() {
            return entity;
        }
        
        public boolean store() throws IOException {
            StoreRequest request = context.getMessageHelper()
                .createStoreRequest(node.getContactAddress(), securityToken, Collections.singleton(entity));
            context.getMessageDispatcher().send(node, request, StoreResponseHandler.this);
            return true;
        }
        
        public void response(Collection<StoreStatusCode> statusCodes) throws IOException {
            // We store one value per request! If the remote Node
            // sends us a different number of StoreStatusCodes back
            // then there is something wrong!
            if (statusCodes.size() != 1) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(node + " sent a wrong number of StoreStatusCodes: " + statusCodes);
                }
                return;
            }
            
            // The returned StoreStatusCode must have the same primary and
            // secondaryKeys as the value we requested to store.
            StoreStatusCode statusCode = statusCodes.iterator().next();
            if (!statusCode.isFor(entity)) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(node + " sent a wrong [" + statusCode + "] for " + entity
                            + "\n" + CollectionUtils.toString(entities));
                }
                return;
            }
            
            this.statusCodes = statusCodes;
        }
        
        public void setTimeout(long time) {
            this.timeout = time;
        }
        
        public long getTimeout() {
            return timeout;
        }
        
        public void setException(Exception exception) {
            this.exception = exception;
        }
        
        public Exception getException() {
            return exception;
        }
        
        public Collection<StoreStatusCode> getStoreStatusCodes() {
            if (statusCodes == null || statusCodes.isEmpty()) {
                statusCodes = Collections.singleton(
                        new StoreStatusCode(entity, StoreResponse.ERROR));
            }
            
            return statusCodes;
        }
    }
}
