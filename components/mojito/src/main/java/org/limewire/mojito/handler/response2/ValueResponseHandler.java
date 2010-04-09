package org.limewire.mojito.handler.response2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.entity.DefaultValueEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.FindValueRequest;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.MessageHelper;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.messages.SecurityTokenProvider;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.DatabaseUtils;
import org.limewire.security.SecurityToken;

public class ValueResponseHandler extends LookupResponseHandler<ValueEntity> {

    private static final Log LOG 
        = LogFactory.getLog(ValueResponseHandler.class);
    
    private static final boolean EXHAUSTIVE = false;
    
    /** The key we're looking for */
    private final EntityKey lookupKey;
    
    /** Collection of EntityKeys */
    private final Collection<EntityKey> entityKeys
        = new ArrayList<EntityKey>();

    /** Collection of DHTValueEntities */
    private final Collection<DHTValueEntity> entities 
        = new ArrayList<DHTValueEntity>();
    
    public ValueResponseHandler(Context context, EntityKey lookupKey) {
        super(context, lookupKey.getPrimaryKey(), -1L, TimeUnit.MILLISECONDS);
        this.lookupKey = lookupKey;
    }

    @Override
    protected void lookup(Contact dst, KUID key, 
            long timeout, TimeUnit unit) throws IOException {
        
        Collection<KUID> noKeys = Collections.emptySet();
        
        MessageHelper messageHelper = context.getMessageHelper();
        FindValueRequest request = messageHelper.createFindValueRequest(
                dst.getContactAddress(), key, 
                noKeys, lookupKey.getDHTValueType());
        
        MessageDispatcher messageDispatcher = context.getMessageDispatcher();
        messageDispatcher.send(dst, request, this, timeout, unit);
    }
    
    @Override
    protected void complete(State state) {
        if (entities.isEmpty() && entityKeys.isEmpty()) {
            setException(new NoSuchValueException(state));
        } else {
            setValue(new DefaultValueEntity(lookupKey, 
                    this.entities, this.entityKeys, state));
        }
    }
    
    @Override
    protected void processResponse0(ResponseMessage message, 
            long time, TimeUnit unit) throws IOException {
        
        if (message instanceof FindNodeResponse) {
            processNodeResponse((FindNodeResponse)message, time, unit);
        } else {
            processValueResponse((FindValueResponse)message, time, unit);
        }
    }
    
    private void processNodeResponse(FindNodeResponse response, 
            long time, TimeUnit unit) throws IOException {
        
        Contact src = response.getContact();
        SecurityToken securityToken = null;
        
        if (response instanceof SecurityTokenProvider) {
            securityToken = ((SecurityTokenProvider)response).getSecurityToken();
        }
        
        Contact[] contacts = response.getNodes().toArray(new Contact[0]);
        processContacts(src, securityToken, contacts, time, unit);
    }
    
    private void processValueResponse(FindValueResponse response, 
            long time, TimeUnit unit) throws IOException {
        
        Contact src = response.getContact();
        
        Collection<KUID> availableSecondaryKeys = response.getSecondaryKeys();
        Collection<? extends DHTValueEntity> entities = response.getDHTValueEntities();
        
        // No keys and no values? In other words the remote Node sent us
        // a FindValueResponse even though it doesn't have a value for
        // the given KUID!? Continue with the lookup if so...!
        if (availableSecondaryKeys.isEmpty() && entities.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(src + " returned neither keys nor values for " + key);
            }
            
            // Continue with the lookup...
            return;
        }
        
        Collection<? extends DHTValueEntity> filtered 
            = DatabaseUtils.filter(lookupKey.getDHTValueType(), entities);
    
        // The filtered Set is empty and the unfiltered isn't?
        // The remote Node send us unrequested Value(s)!
        // Continue with the lookup if so...!
        if (filtered.isEmpty() && !entities.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(src + " returned unrequested types of values for " + key);
            }
            
            // Continue with the lookup...
            return;
        }
        
        this.entities.addAll(filtered);
        
        for (KUID secondaryKey : availableSecondaryKeys) {
            EntityKey entityKey = EntityKey.createEntityKey(
                    src, key, secondaryKey, lookupKey.getDHTValueType());
            
            this.entityKeys.add(entityKey);
        }
        
        if (!EXHAUSTIVE) {
            State state = getState();
            setValue(new DefaultValueEntity(lookupKey, 
                    this.entities, this.entityKeys, state));
        }
    }
    
    public static class NoSuchValueException extends IOException {
        
        private final State state;
        
        public NoSuchValueException(State state) {
            this.state = state;
        }
        
        public State getState() {
            return state;
        }
    }
}
