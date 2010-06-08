package org.limewire.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.ValueKey;
import org.limewire.mojito.db.ValueTuple;
import org.limewire.mojito.db.ValueType;
import org.limewire.mojito.entity.DefaultValueEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.exceptions.NoSuchValueException;
import org.limewire.mojito.message.MessageHelper;
import org.limewire.mojito.message.NodeResponse;
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.message.ValueRequest;
import org.limewire.mojito.message.ValueResponse;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.DatabaseUtils;
import org.limewire.security.SecurityToken;

/**
 * An implementation of {@link ResponseHandler} that is managing 
 * a <tt>FIND_VALUE</tt> lookup process.
 */
public class ValueResponseHandler extends LookupResponseHandler<ValueEntity> {

    private static final Log LOG 
        = LogFactory.getLog(ValueResponseHandler.class);
    
    private static final boolean EXHAUSTIVE = false;
    
    /** The key we're looking for */
    private final ValueKey lookupKey;
    
    /** Collection of {@link ValueKey}s */
    private final List<ValueKey> entityKeys
        = new ArrayList<ValueKey>();

    /** Collection of {@link ValueTuple}ies */
    private final List<ValueTuple> entities 
        = new ArrayList<ValueTuple>();
    
    public ValueResponseHandler(Context context, 
            ValueKey lookupKey, 
            long timeout, TimeUnit unit) {
        super(Type.FIND_VALUE, context, 
                lookupKey.getPrimaryKey(), timeout, unit);
        this.lookupKey = lookupKey;
    }

    @Override
    protected void lookup(Contact dst, KUID key, 
            long timeout, TimeUnit unit) throws IOException {
        
        KUID contactId = dst.getContactId();
        SocketAddress addr = dst.getContactAddress();
        
        KUID[] noKeys = new KUID[0];
        
        MessageHelper messageHelper = context.getMessageHelper();
        ValueRequest request = messageHelper.createFindValueRequest(
                addr, key, noKeys, lookupKey.getValueType());
        
        long adaptiveTimeout = dst.getAdaptativeTimeout(timeout, unit);
        send(contactId, addr, request, adaptiveTimeout, unit);
    }
    
    @Override
    protected void complete(State state) {
        if (entities.isEmpty() && entityKeys.isEmpty()) {
            setException(new DefaultNoSuchValueException(state));
        } else {
            setValue(new DefaultValueEntity(lookupKey, 
                    this.entities.toArray(new ValueTuple[0]), 
                    this.entityKeys.toArray(new ValueKey[0]), state));
        }
    }
    
    @Override
    protected void processResponse0(RequestHandle request, 
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
        ValueTuple[] entities = response.getValues();
        
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
        
        ValueType valueType = lookupKey.getValueType();
        ValueTuple[] filtered = DatabaseUtils.filter(valueType, entities);
    
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
            ValueKey entityKey = ValueKey.createValueKey(
                    src, lookupId, secondaryKey, lookupKey.getValueType());
            
            this.entityKeys.add(entityKey);
        }
        
        if (!EXHAUSTIVE) {
            State state = getState();
            setValue(new DefaultValueEntity(lookupKey, 
                    this.entities.toArray(new ValueTuple[0]), 
                    this.entityKeys.toArray(new ValueKey[0]), state));
        }
    }
    
    public static class DefaultNoSuchValueException extends NoSuchValueException {
        
        private final State state;
        
        public DefaultNoSuchValueException(State state) {
            this.state = state;
        }
        
        public State getState() {
            return state;
        }
    }
}
