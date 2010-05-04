package org.limewire.mojito2.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.StatusCode;
import org.limewire.mojito2.entity.DefaultStoreEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.message.MessageHelper;
import org.limewire.mojito2.message.ResponseMessage;
import org.limewire.mojito2.message.StoreRequest;
import org.limewire.mojito2.message.StoreResponse;
import org.limewire.mojito2.message.StoreStatusCode;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.Database;
import org.limewire.mojito2.util.ContactUtils;
import org.limewire.mojito2.util.MaxStack;
import org.limewire.security.SecurityToken;

public class StoreResponseHandler extends AbstractResponseHandler<StoreEntity> {

    private static final Log LOG 
        = LogFactory.getLog(StoreResponseHandler.class);
    
    private static final int PARALLELISM = 4;
    
    private final Entry<Contact, SecurityToken>[] contacts;
    
    private final DHTValueEntity[] values;
    
    private final MaxStack processCounter 
        = new MaxStack(PARALLELISM);
    
    private final List<StoreProcess> processes 
        = new ArrayList<StoreProcess>();
    
    private final Map<ProcessKey, StoreProcess> active 
        = new HashMap<ProcessKey, StoreProcess>();
    
    private final Map<Contact, List<StoreStatusCode>> codes 
        = new HashMap<Contact, List<StoreStatusCode>>();
    
    private final AtomicBoolean once = new AtomicBoolean(false);
    
    public StoreResponseHandler(Context context, 
            DHTValueEntity[] entities, 
            long timeout, TimeUnit unit) {
        this(context, null, entities, timeout, unit);
    }
    
    public StoreResponseHandler(Context context, 
            Entry<Contact, SecurityToken>[] contacts, 
            DHTValueEntity[] values, 
            long timeout, TimeUnit unit) {
        super(context, timeout, unit);
        
        this.contacts = contacts;
        this.values = values;
    }
    
    private void init(Entry<Contact, SecurityToken>[] contacts, 
            DHTValueEntity[] entities) {
        
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
    protected void start() throws IOException {
        if (contacts != null) {
            store(contacts);
        }
    }
    
    public void store(Entry<Contact, SecurityToken>[] contacts) throws IOException {
        
        if (once.getAndSet(true)) {
            throw new IllegalStateException();
        }
        
        init(contacts, values);
        process(0);
    }
    
    private synchronized void process(int decrement) throws IOException {
        try {
            preProcess(decrement);
            while (processCounter.hasFree()) {
                if (processes.isEmpty()) {
                    break;
                }
                
                StoreProcess process 
                    = processes.remove(0);
                
                boolean done = store(process);
                
                if (!done) {
                    processCounter.push();
                }
            }
        } finally {
            postProcess();
        }
    }
    
    private synchronized void preProcess(int value) {
        processCounter.pop(value);
    }
    
    private synchronized void postProcess() {
        int count = processCounter.poll();
        if (count == 0) {
            complete();
        }
    }
    
    private synchronized void complete() {
        long time = getTime(TimeUnit.MILLISECONDS);
        
        Contact[] contacts = codes.keySet().toArray(new Contact[0]);
        setValue(new DefaultStoreEntity(contacts, time, TimeUnit.MILLISECONDS));
    }
    
    private synchronized boolean store(StoreProcess process) throws IOException {
        if (!process.store()) {
            active.put(new ProcessKey(process), process);
            return false;
        }
        
        return true;
    }

    @Override
    protected void processResponse(RequestHandle request, 
            ResponseMessage response, long time, TimeUnit unit) throws IOException {
        
        try {
            processResponse0(request, (StoreResponse)response, time, unit);
        } finally {
            process(1);
        }
    }
    
    private void processResponse0(RequestHandle request, 
            StoreResponse response, long time, TimeUnit unit) throws IOException {
        
        Contact src = response.getContact();
        StoreStatusCode[] codes = response.getStoreStatusCodes();
        
        // We store one value per request! If the remote Node
        // sends us a different number of StoreStatusCodes back
        // then there is something wrong!
        if (codes.length != 1) {
            if (LOG.isErrorEnabled()) {
                LOG.error(response.getContact() 
                        + " sent a wrong number of StoreStatusCodes: " + codes);
            }
            
            return;
        }
        
        StoreStatusCode code = codes[0];
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
        
        addStoreStatusCode(src, entity, code.getStatusCode());
    }
    
    @Override
    protected void processTimeout(RequestHandle request, 
            long time, TimeUnit unit) throws IOException {
        try {
            processTimeout0(request, time, unit);
        } finally {
            process(1);
        }
    }
    
    private void processTimeout0(RequestHandle handle, 
            long time, TimeUnit unit) throws IOException {
        
        KUID contactId = handle.getContactId();
        SocketAddress dst = handle.getAddress();
        
        StoreRequest request = (StoreRequest)handle.getRequest();
        DHTValueEntity[] entities = request.getValueEntities();
        
        DHTValueEntity entity = entities[0];
        
        if (LOG.isInfoEnabled()) {
            LOG.info("Couldn't store " + entity + " at " 
                    + ContactUtils.toString(contactId, dst));
        }
        
        StoreProcess process = active.remove(new ProcessKey(contactId, entity));
        if (process == null) {
            return;
        }
        
        addStoreStatusCode(process.dst, entity, StoreStatusCode.ERROR);
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
            
            addStoreStatusCode(dst, entity, stored ? StoreStatusCode.OK : StoreStatusCode.ERROR);
            
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
            
            KUID contactId = dst.getNodeID();
            SocketAddress addr = dst.getContactAddress();
            
            
            MessageHelper messageHelper = context.getMessageHelper();
            StoreRequest request = messageHelper.createStoreRequest(
                    addr, securityToken, new DHTValueEntity[] { entity });
            
            long adaptiveTimeout = dst.getAdaptativeTimeout(timeout, unit);
            send(contactId, addr, request, adaptiveTimeout, unit);
        
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

