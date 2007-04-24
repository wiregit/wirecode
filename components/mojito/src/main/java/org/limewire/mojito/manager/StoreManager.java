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

package org.limewire.mojito.manager;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureTask;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTBackendException;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.handler.response.AbstractResponseHandler;
import org.limewire.mojito.handler.response.FindNodeResponseHandler;
import org.limewire.mojito.handler.response.FindValueResponseHandler;
import org.limewire.mojito.handler.response.LookupResponseHandler;
import org.limewire.mojito.handler.response.StoreResponseHandler;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.messages.SecurityTokenProvider;
import org.limewire.mojito.result.LookupResult;
import org.limewire.mojito.result.Result;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.EntryImpl;
import org.limewire.mojito.util.ContactFilter;
import org.limewire.security.SecurityToken;

/**
 * The StoreManager class manages 
 */
public class StoreManager extends AbstractManager<StoreResult> {
    
    public StoreManager(Context context) {
        super(context);
    }
    
    /**
     * Stores a collection of DHTValues on the DHT. All DHTValues
     * must have the same valueId!
     */
    public DHTFuture<StoreResult> store(Collection<? extends DHTValueEntity> values) {
        StoreProcess task = new StoreProcess(values);
        StoreFuture future = new StoreFuture(task);
        
        context.getDHTExecutorService().execute(future);
        return future;
    }
    
    /**
     * Stores a collection of DHTValues at the given Contact.
     */
    public DHTFuture<StoreResult> store(Contact node, SecurityToken securityToken, 
            Collection<? extends DHTValueEntity> values) {
        
        Entry<Contact, SecurityToken> entry 
            = new EntryImpl<Contact, SecurityToken>(node, securityToken);
        StoreProcess task = new StoreProcess(entry, values);
        
        StoreFuture future = new StoreFuture(task);
        context.getDHTExecutorService().execute(future);
        return future;
    }
    
    /**
     * A store specific implementation of DHTFuture
     */
    private class StoreFuture extends DHTFutureTask<StoreResult> {
        
        public StoreFuture(DHTTask<StoreResult> task) {
            super(context, task);
        }
    }
    
    /**
     * 
     */
    private class StoreProcess implements DHTTask<StoreResult> {
        
        private final List<DHTTask> tasks = new ArrayList<DHTTask>();
        
        private boolean cancelled = false;
        
        private DHTTask.Callback<StoreResult> callback;
        
        private final KUID valueId;
        
        private final Entry<? extends Contact, ? extends SecurityToken> node;
        
        private final Collection<? extends DHTValueEntity> values;
        
        public StoreProcess(Collection<? extends DHTValueEntity> values) {
            this(null, values);
        }
        
        public StoreProcess(Entry<? extends Contact, ? extends SecurityToken> node,
                Collection<? extends DHTValueEntity> values) {
            
            this.values = values;
            this.node = node;
            
            if (node != null && node.getKey() == null) {
                throw new IllegalArgumentException("Contact is null");
            }
            
            if (values.isEmpty()) {
                throw new IllegalArgumentException("No Values to store");
            }
            
            KUID valueId = null;
            if (node == null) {
                for (DHTValueEntity value : values) {
                    if (valueId == null) {
                        valueId = value.getPrimaryKey();
                    }
                    
                    if (!valueId.equals(value.getPrimaryKey())) {
                        throw new IllegalArgumentException("All DHTValues must have the same Value ID");
                    }
                }
            }
            
            this.valueId = valueId;
        }

        public long getLockTimeout() {
            return NetworkSettings.STORE_TIMEOUT.getValue();
        }

        public void start(DHTTask.Callback<StoreResult> callback) {
            this.callback = callback;
            
            // Regular store operation
            if (node == null) {
                findNearestNodes();
                
            // Get the SecurityToken and store the value(s) 
            // at the given Node 
            } else if (node.getValue() == null
                    && KademliaSettings.STORE_REQUIRES_SECURITY_TOKEN.getValue()) {
                doGetSecurityToken();
                
            } else {
                doStoreOnPath(Collections.singleton(node));
            }
        }
        
        private void findNearestNodes() {
            DHTTask.Callback<LookupResult> c = new DHTTask.Callback<LookupResult>() {
                public void setReturnValue(LookupResult value) {
                    handleNearestNodes(value);
                }
                
                public void setException(Throwable t) {
                    callback.setException(t);
                }
            };
            
            // Do a lookup for the k-closest Nodes where we're
            // going to store the value
            LookupResponseHandler<LookupResult> handler = createLookupResponseHandler();
            
            // Use only alive Contacts from the RouteTable
            handler.setSelectAliveNodesOnly(true);
            
            start(handler, c);
        }
        
        private void handleNearestNodes(LookupResult value) {
            doStoreOnPath(value.getEntryPath());
        }
        
        private void doGetSecurityToken() {
            DHTTask.Callback<GetSecurityTokenResult> c = new DHTTask.Callback<GetSecurityTokenResult>() {
                public void setReturnValue(GetSecurityTokenResult value) {
                    handleSecurityToken(value);
                }
                
                public void setException(Throwable t) {
                    callback.setException(t);
                }
            };
            
            GetSecurityTokenHandler handler 
                = new GetSecurityTokenHandler(context, node.getKey());
            
            start(handler, c);
        }
        
        private void handleSecurityToken(GetSecurityTokenResult result) {
            SecurityToken securityToken = result.getSecurityToken();
            if (securityToken == null) {
                callback.setException(new DHTException("Coult not get SecurityToken from " + node));
            } else {
                Entry<Contact, SecurityToken> entry 
                    = new EntryImpl<Contact, SecurityToken>(node.getKey(), securityToken);
                
                doStoreOnPath(Collections.singleton(entry));
            }
        }
        
        private void doStoreOnPath(Collection<? extends Entry<? extends Contact, ? extends SecurityToken>> path) {
            // And store the values along the path
            StoreResponseHandler handler 
                = new StoreResponseHandler(context, path, values);
            start(handler, callback);
        }
        
        private <T> void start(DHTTask<T> task, 
                DHTTask.Callback<T> c) {
            synchronized (tasks) {
                if (!cancelled) {
                    tasks.add(task);
                    task.start(c);
                }
            }
        }
        
        public void cancel() {
            synchronized (tasks) {
                cancelled = true;
                for (DHTTask<?> task : tasks) {
                    task.cancel();
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        private LookupResponseHandler<LookupResult> createLookupResponseHandler() {
            LookupResponseHandler<? extends LookupResult> handler = null;
            if (KademliaSettings.FIND_NODE_FOR_SECURITY_TOKEN.getValue()) {
                handler = new FindNodeResponseHandler(context, valueId);
            } else {
                handler = new FindValueResponseHandler(context, valueId, DHTValueType.ANY);
            }
            return (LookupResponseHandler<LookupResult>)handler;
        }
    }
    
    /**
     * GetSecurityTokenHandler tries to get the SecurityToken of a Node
     */
    private static class GetSecurityTokenHandler extends AbstractResponseHandler<GetSecurityTokenResult> {
        
        private static final Log LOG = LogFactory.getLog(GetSecurityTokenHandler.class);
        
        private final Contact node;
        
        private GetSecurityTokenHandler(Context context, Contact node) {
            super(context);
            this.node = node;
        }

        @Override
        protected void start() throws DHTException {
            RequestMessage request = createLookupRequest();
            
            try {
                context.getMessageDispatcher().send(node, request, this);
            } catch (IOException err) {
                throw new DHTException(err);
            }
        }
        
        private RequestMessage createLookupRequest() {
            if (KademliaSettings.FIND_NODE_FOR_SECURITY_TOKEN.getValue()) {
                return context.getMessageHelper()
                        .createFindNodeRequest(node.getContactAddress(), node.getNodeID());
            } else {
                Collection<KUID> noKeys = Collections.emptySet();
                return context.getMessageHelper()
                    .createFindValueRequest(node.getContactAddress(), node.getNodeID(), noKeys, DHTValueType.ANY);
            }
        }
        
        @Override
        protected void response(ResponseMessage message, long time) throws IOException {
            
            if (message instanceof FindNodeResponse) {
                FindNodeResponse response = (FindNodeResponse)message;
                
                Contact sender = response.getContact();
                Collection<? extends Contact> nodes = response.getNodes();
                
                ContactFilter filter = new ContactFilter(context, response.getContact());
                
                // We did a FIND_NODE lookup use the info
                // to fill/update our routing table
                for(Contact node : nodes) {
                    
                    if (!filter.isValidContact(node)) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Dropping invalid Contact " + node + " from " + sender);
                        }
                        continue;
                    }
                    
                    assert (node.isAlive() == false);
                    context.getRouteTable().add(node);
                }
            }
            
            SecurityToken securityToken = null;
            if (message instanceof SecurityTokenProvider) {
                securityToken = ((SecurityTokenProvider)message).getSecurityToken();
            }
            
            setReturnValue(new GetSecurityTokenResult(securityToken));
        }
        
        @Override
        protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
            fireTimeoutException(nodeId, dst, message, time);
        }

        @Override
        protected void error(KUID nodeId, SocketAddress dst, RequestMessage message, IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Getting the SecurityToken from " + ContactUtils.toString(nodeId, dst) + " failed", e);
            }
            
            setException(new DHTBackendException(nodeId, dst, message, e));
        }
    }
    
    private static class GetSecurityTokenResult implements Result {
        
        private final SecurityToken securityToken;
        
        public GetSecurityTokenResult(SecurityToken securityToken) {
            this.securityToken = securityToken;
        }
        
        public SecurityToken getSecurityToken() {
            return securityToken;
        }
    }
}
