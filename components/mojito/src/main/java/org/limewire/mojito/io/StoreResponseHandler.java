package org.limewire.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.StatusCode;
import org.limewire.mojito.entity.DefaultStoreEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.message.MessageHelper;
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.message.StoreRequest;
import org.limewire.mojito.message.StoreResponse;
import org.limewire.mojito.message.StoreStatusCode;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.StoreSettings;
import org.limewire.mojito.storage.Database;
import org.limewire.mojito.storage.ValueTuple;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.MaxStack;
import org.limewire.security.SecurityToken;

/**
 * An implementation of {@link ResponseHandler} that is managing the
 * storing of value at a remote node.
 */
public class StoreResponseHandler extends AbstractResponseHandler<StoreEntity> {

    private static final Log LOG 
        = LogFactory.getLog(StoreResponseHandler.class);
    
    private final Entry<Contact, SecurityToken>[] contacts;
    
    private final ValueTuple entity;
    
    private final MaxStack processCounter 
        = new MaxStack(StoreSettings.PARALLEL_STORES.getValue());
    
    private final List<StoreProcess> processes 
        = new ArrayList<StoreProcess>();
    
    private final Map<ProcessKey, StoreProcess> active 
        = new HashMap<ProcessKey, StoreProcess>();
    
    private final Map<Contact, List<StoreStatusCode>> codes 
        = new HashMap<Contact, List<StoreStatusCode>>();
    
    private final AtomicBoolean once = new AtomicBoolean(false);
    
    private final AtomicInteger complete = new AtomicInteger();
    
    public StoreResponseHandler(Context context, 
            ValueTuple entity, 
            long timeout, TimeUnit unit) {
        this(context, null, entity, timeout, unit);
    }
    
    public StoreResponseHandler(Context context, 
            Entry<Contact, SecurityToken>[] contacts, 
            ValueTuple entity, 
            long timeout, TimeUnit unit) {
        super(context, timeout, unit);
        
        this.contacts = contacts;
        this.entity = entity;
    }
    
    private void init(Entry<Contact, SecurityToken>[] contacts) {
        
        Contact localhost = context.getLocalhost();
        
        for (Entry<Contact, SecurityToken> entry : contacts) {
            Contact contact = entry.getKey();
            SecurityToken securityToken = entry.getValue();
            
            if (contact.equals(localhost)) {
                processes.add(new LocalStoreProcess(
                        contact, securityToken, entity));
            } else {
                processes.add(new RemoteStoreProcess(
                        contact, securityToken, entity));
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
        
        init(contacts);
        process(0);
    }
    
    private synchronized void process(int decrement) throws IOException {
        try {
            preProcess(decrement);
            while (processCounter.hasFree()) {
                if (processes.isEmpty()) {
                    break;
                }
                
                if ((KademliaSettings.K - complete.get() - 1) < processCounter.poll()) {
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
        
        ValueTuple entity = process.entity;
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
        ValueTuple[] entities = request.getValues();
        
        ValueTuple entity = entities[0];
        
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
            ValueTuple entity, StatusCode code) {
        
        List<StoreStatusCode> list = codes.get(dst);
        if (list == null) {
            list = new ArrayList<StoreStatusCode>();
            codes.put(dst, list);
        }
        list.add(new StoreStatusCode(entity, code));
        
        if (code.equals(StoreStatusCode.OK)) {
            complete.incrementAndGet();
        }
    }
    
    /**
     * An abstract class to store values.
     */
    private abstract class StoreProcess {
        
        protected final Contact dst;
        
        protected final SecurityToken securityToken;
        
        protected final ValueTuple entity;
        
        public StoreProcess(Contact dst, SecurityToken securityToken, 
                ValueTuple entity) {
            this.dst = dst;
            this.securityToken = securityToken;
            this.entity = entity;
        }
        
        /**
         * Stores a {@link ValueTuple} at a remote node.
         */
        public abstract boolean store() throws IOException;
    }
    
    /**
     * An implementation of {@link StoreProcess} that stores a
     * value in the local database.
     */
    private class LocalStoreProcess extends StoreProcess {

        public LocalStoreProcess(Contact dst, 
                SecurityToken securityToken, 
                ValueTuple entity) {
            super(dst, securityToken, entity);
        }

        @Override
        public boolean store() {
            Database database = context.getDatabase();
            boolean stored = database.store(entity);
            
            addStoreStatusCode(dst, entity, stored ? 
                    StoreStatusCode.OK : StoreStatusCode.ERROR);
            
            return true;
        }
    }
    
    /**
     * An implementation of {@link StoreProcess} that stores a
     * value at a remote node.
     */
    private class RemoteStoreProcess extends StoreProcess {

        public RemoteStoreProcess(Contact dst, 
                SecurityToken securityToken, ValueTuple entity) {
            super(dst, securityToken, entity);
        }

        @Override
        public boolean store() throws IOException {
            
            KUID contactId = dst.getContactId();
            SocketAddress addr = dst.getContactAddress();
            
            
            MessageHelper messageHelper = context.getMessageHelper();
            StoreRequest request = messageHelper.createStoreRequest(
                    addr, securityToken, new ValueTuple[] { entity });
            
            long adaptiveTimeout = dst.getAdaptativeTimeout(timeout, unit);
            send(contactId, addr, request, adaptiveTimeout, unit);
        
            return false;
        }
    }
    
    /**
     * A handle that is used as a key for a {@link Map}.
     */
    private static class ProcessKey {
        
        private final KUID contactId;
        
        private final KUID primaryKey;
        
        private final KUID secondaryKey;
        
        private final int hashCode;
        
        public ProcessKey(StoreProcess process) {
            this(process.dst.getContactId(), 
                    process.entity.getPrimaryKey(), 
                    process.entity.getSecondaryKey());
        }
        
        public ProcessKey(Contact src, StoreStatusCode code) {
            this(src.getContactId(), code.getPrimaryKey(), 
                    code.getSecondaryKey());
        }
        
        public ProcessKey(KUID nodeId, ValueTuple entity) {
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

