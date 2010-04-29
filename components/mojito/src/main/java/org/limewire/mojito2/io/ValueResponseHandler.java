package org.limewire.mojito2.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.entity.DefaultValueEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.util.DatabaseUtils;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.message.MessageHelper;
import org.limewire.mojito2.message.NodeResponse;
import org.limewire.mojito2.message.RequestMessage;
import org.limewire.mojito2.message.ResponseMessage;
import org.limewire.mojito2.message.ValueRequest;
import org.limewire.mojito2.message.ValueResponse;
import org.limewire.mojito2.routing.Contact;
import org.limewire.security.SecurityToken;

public class ValueResponseHandler extends LookupResponseHandler<ValueEntity> {

    private static final Log LOG 
        = LogFactory.getLog(ValueResponseHandler.class);
    
    private static final boolean EXHAUSTIVE = false;
    
    /** The key we're looking for */
    private final EntityKey lookupKey;
    
    /** Collection of {@link EntityKey}s */
    private final List<EntityKey> entityKeys
        = new ArrayList<EntityKey>();

    /** Collection of {@link DHTValueEntity}ies */
    private final List<DHTValueEntity> entities 
        = new ArrayList<DHTValueEntity>();
    
    public ValueResponseHandler(Context context, 
            EntityKey lookupKey, 
            long timeout, TimeUnit unit) {
        super(context, lookupKey.getPrimaryKey(), timeout, unit);
        this.lookupKey = lookupKey;
    }

    @Override
    protected void lookup(Contact dst, KUID key, 
            long timeout, TimeUnit unit) throws IOException {
        
        KUID contactId = dst.getNodeID();
        SocketAddress addr = dst.getContactAddress();
        
        KUID[] noKeys = new KUID[0];
        
        MessageHelper messageHelper = context.getMessageHelper();
        ValueRequest request = messageHelper.createFindValueRequest(
                addr, key, noKeys, lookupKey.getDHTValueType());
        
        long adaptiveTimeout = dst.getAdaptativeTimeout(timeout, unit);
        send(contactId, addr, request, adaptiveTimeout, unit);
    }
    
    @Override
    protected void complete(State state) {
        if (entities.isEmpty() && entityKeys.isEmpty()) {
            setException(new NoSuchValueException(state));
        } else {
            setValue(new DefaultValueEntity(lookupKey, 
                    this.entities.toArray(new DHTValueEntity[0]), 
                    this.entityKeys.toArray(new EntityKey[0]), state));
        }
    }
    
    @Override
    protected void processResponse0(RequestMessage request, 
            ResponseMessage response, long time, TimeUnit unit) throws IOException {
        
        if (response instanceof NodeResponse) {
            processNodeResponse((NodeResponse)response, time, unit);
        } else {
            processValueResponse((ValueResponse)response, time, unit);
        }
    }
    
    private void processNodeResponse(NodeResponse response, 
            long time, TimeUnit unit) throws IOException {
        
        Contact src = response.getContact();
        SecurityToken securityToken = response.getSecurityToken();
        
        Contact[] contacts = response.getContacts();
        processContacts(src, securityToken, contacts, time, unit);
    }
    
    private void processValueResponse(ValueResponse response, 
            long time, TimeUnit unit) throws IOException {
        
        Contact src = response.getContact();
        
        KUID[] availableSecondaryKeys = response.getSecondaryKeys();
        DHTValueEntity[] entities = response.getValueEntities();
        
        // No keys and no values? In other words the remote Node sent us
        // a FindValueResponse even though it doesn't have a value for
        // the given KUID!? Continue with the lookup if so...!
        if (availableSecondaryKeys.length == 0 && entities.length == 0) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(src + " returned neither keys nor values for " + lookupId);
            }
            
            // Continue with the lookup...
            return;
        }
        
        DHTValueType valueType = lookupKey.getDHTValueType();
        DHTValueEntity[] filtered = DatabaseUtils.filter(valueType, entities);
    
        // The filtered Set is empty and the unfiltered isn't?
        // The remote Node send us unrequested Value(s)!
        // Continue with the lookup if so...!
        if (filtered.length == 0 && entities.length != 0) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(src + " returned unrequested types of values for " + lookupId);
            }
            
            // Continue with the lookup...
            return;
        }
        
        this.entities.addAll(Arrays.asList(filtered));
        
        for (KUID secondaryKey : availableSecondaryKeys) {
            EntityKey entityKey = EntityKey.createEntityKey(
                    src, lookupId, secondaryKey, lookupKey.getDHTValueType());
            
            this.entityKeys.add(entityKey);
        }
        
        if (!EXHAUSTIVE) {
            State state = getState();
            setValue(new DefaultValueEntity(lookupKey, 
                    this.entities.toArray(new DHTValueEntity[0]), 
                    this.entityKeys.toArray(new EntityKey[0]), state));
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
