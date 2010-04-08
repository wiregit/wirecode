package org.limewire.mojito.handler.response2;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.StatusCode;
import org.limewire.mojito.concurrent2.AsyncFuture;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.entity.DefaultStoreEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.messages.MessageHelper;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.messages.StoreRequest;
import org.limewire.mojito.messages.StoreResponse;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.security.SecurityToken;

public class StoreResponseHandler extends AbstractResponseHandler<StoreEntity> {

    private static final Log LOG 
        = LogFactory.getLog(StoreResponseHandler.class);
    
    private static final int PARALLELISM = 4;
    
    private final ProcessCounter processCounter 
        = new ProcessCounter(PARALLELISM);
    
    private final List<StoreProcess> processes 
        = new ArrayList<StoreProcess>();
    
    private final Map<ProcessKey, StoreProcess> active 
        = new HashMap<ProcessKey, StoreProcess>();
    
    private final Map<Contact, List<StoreStatusCode>> codes 
        = new HashMap<Contact, List<StoreStatusCode>>();
    
    private long startTime = -1L;
    
    public StoreResponseHandler(Context context, 
            Entry<Contact, SecurityToken>[] contacts, 
            DHTValueEntity[] entities) {
        super(context);
        
        for (Entry<Contact, SecurityToken> entry : contacts) {
            Contact node = entry.getKey();
            SecurityToken securityToken = entry.getValue();
            
            for (DHTValueEntity entity : entities) {
                if (context.isLocalNode(node)) {
                    processes.add(new LocalStoreProcess(
                            node, securityToken, entity));
                } else {
                    processes.add(new RemoteStoreProcess(
                            node, securityToken, entity));
                }
            }
        }
    }

    @Override
    protected void doStart(AsyncFuture<StoreEntity> future) throws IOException {
        startTime = System.currentTimeMillis();
        
        process(0);
    }
    
    private synchronized void process(int decrement) throws IOException {
        try {
            preProcess(decrement);
            while (processCounter.hasNext()) {
                if (processes.isEmpty()) {
                    break;
                }
                
                StoreProcess process 
                    = processes.remove(0);
                
                boolean done = store(process);
                
                if (!done) {
                    processCounter.increment();
                }
            }
        } finally {
            postProcess();
        }
    }
    
    private synchronized void preProcess(int decrement) {
        processCounter.decrement(decrement);
    }
    
    private synchronized void postProcess() {
        int count = processCounter.get();
        if (count == 0) {
            complete();
        }
    }
    
    private synchronized void complete() {
        long time = System.currentTimeMillis() - startTime;
        setValue(new DefaultStoreEntity(time, TimeUnit.MILLISECONDS));
    }
    
    private synchronized boolean store(StoreProcess process) throws IOException {
        if (!process.store()) {
            active.put(new ProcessKey(process), process);
            return false;
        }
        
        return true;
    }

    @Override
    protected void processResponse(ResponseMessage message, 
            long time, TimeUnit unit) throws IOException {
        try {
            processResponse0(message, time, unit);
        } finally {
            process(1);
        }
    }
    
    private void processResponse0(ResponseMessage message, 
            long time, TimeUnit unit) throws IOException {
        
        StoreResponse response = (StoreResponse)message;
        
        Contact src = response.getContact();
        Collection<StoreStatusCode> codes = response.getStoreStatusCodes();
        
        // We store one value per request! If the remote Node
        // sends us a different number of StoreStatusCodes back
        // then there is something wrong!
        if (codes.size() != 1) {
            if (LOG.isErrorEnabled()) {
                LOG.error(response.getContact() 
                        + " sent a wrong number of StoreStatusCodes: " + codes);
            }
            return;
        }
        
        StoreStatusCode code = codes.iterator().next();
        StoreProcess process = active.remove(new ProcessKey(src, code));
        if (process == null) {
            return;
        }
        
        DHTValueEntity entity = process.entity;
        if (!code.isFor(entity)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(src + " sent a wrong [" + code + "] for " + entity);
            }
            return;
        }
    }
    
    @Override
    protected void processTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time, TimeUnit unit) throws IOException {
        try {
            processTimeout0(nodeId, dst, message, time, unit);
        } finally {
            process(1);
        }
    }
    
    private void processTimeout0(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time, TimeUnit unit) throws IOException {
        
        StoreRequest request = (StoreRequest)message;
        
        DHTValueEntity entity = request.getDHTValueEntities().iterator().next();
        
        if (LOG.isInfoEnabled()) {
            LOG.info("Couldn't store " + entity + " at " 
                    + ContactUtils.toString(nodeId, dst));
        }
        
        StoreProcess process = active.remove(new ProcessKey(nodeId, entity));
        if (process == null) {
            return;
        }
        
        addStoreStatusCode(process.dst, entity, StoreResponse.ERROR);
    }
    
    private void addStoreStatusCode(Contact dst, 
            DHTValueEntity entity, StatusCode code) {
        
        List<StoreStatusCode> list = codes.get(dst);
        if (list == null) {
            list = new ArrayList<StoreStatusCode>();
            codes.put(dst, list);
        }
        list.add(new StoreStatusCode(entity, code));
    }
    
    private abstract class StoreProcess {
        
        protected final Contact dst;
        
        protected final SecurityToken securityToken;
        
        protected final DHTValueEntity entity;
        
        public StoreProcess(Contact dst, SecurityToken securityToken, 
                DHTValueEntity entity) {
            this.dst = dst;
            this.securityToken = securityToken;
            this.entity = entity;
        }
        
        public abstract boolean store() throws IOException;
    }
    
    private class LocalStoreProcess extends StoreProcess {

        public LocalStoreProcess(Contact dst, 
                SecurityToken securityToken, 
                DHTValueEntity entity) {
            super(dst, securityToken, entity);
        }

        @Override
        public boolean store() {
            Database database = context.getDatabase();
            boolean stored = database.store(entity);
            
            addStoreStatusCode(dst, entity, stored ? StoreResponse.OK : StoreResponse.ERROR);
            
            return true;
        }
    }
    
    private class RemoteStoreProcess extends StoreProcess {

        public RemoteStoreProcess(Contact dst, 
                SecurityToken securityToken, DHTValueEntity entity) {
            super(dst, securityToken, entity);
        }

        @Override
        public boolean store() throws IOException {
            
            MessageHelper messageHelper = context.getMessageHelper();
            StoreRequest request = messageHelper.createStoreRequest(
                    dst.getContactAddress(), 
                    securityToken, 
                    Collections.singleton(entity));
        
            MessageDispatcher messageDispatcher = context.getMessageDispatcher();
            messageDispatcher.send(dst, request, StoreResponseHandler.this);
        
            return false;
        }
    }
    
    private static class ProcessKey {
        
        private final KUID contactId;
        
        private final KUID primaryKey;
        
        private final KUID secondaryKey;
        
        private final int hashCode;
        
        public ProcessKey(StoreProcess process) {
            this(process.dst.getNodeID(), 
                    process.entity.getPrimaryKey(), 
                    process.entity.getSecondaryKey());
        }
        
        public ProcessKey(Contact src, StoreStatusCode code) {
            this(src.getNodeID(), code.getPrimaryKey(), 
                    code.getSecondaryKey());
        }
        
        public ProcessKey(KUID nodeId, DHTValueEntity entity) {
            this(nodeId, entity.getPrimaryKey(), 
                    entity.getSecondaryKey());
        }
        
        public ProcessKey(KUID contactId, 
                KUID primaryKey, KUID secondaryKey) {
            
            this.contactId = contactId;
            this.primaryKey = primaryKey;
            this.secondaryKey = secondaryKey;
            
            int hashCode = 0;
            
            hashCode += 31 * contactId.hashCode();
            hashCode += 31 * primaryKey.hashCode();
            hashCode += 31 * secondaryKey.hashCode();
            
            this.hashCode = hashCode;
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof ProcessKey)) {
                return false;
            }
            
            ProcessKey other = (ProcessKey)o;
            return contactId.equals(other.contactId)
                && primaryKey.equals(other.primaryKey)
                && secondaryKey.equals(other.secondaryKey);
        }
    }
}
