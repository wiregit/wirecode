package org.limewire.mojito.handler.request;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context2;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.entity.DefaultValueEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.handler.response.AbstractResponseHandler2;
import org.limewire.mojito.messages.FindValueRequest;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.MessageHelper2;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.DatabaseUtils;

public class GetValueResponseHandler2 extends AbstractResponseHandler2<ValueEntity> {

    private static final Log LOG 
        = LogFactory.getLog(GetValueResponseHandler2.class);
    
    private final EntityKey lookupKey;
    
    public GetValueResponseHandler2(Context2 context, 
            EntityKey lookupKey, long timeout, TimeUnit unit) {
        super(context, timeout, unit);
        
        this.lookupKey = lookupKey;
    }

    @Override
    protected void start() throws IOException {
        Contact node = lookupKey.getContact();
        KUID primaryKey = lookupKey.getPrimaryKey();
        KUID secondaryKey = lookupKey.getSecondaryKey();
        DHTValueType valueType = lookupKey.getDHTValueType();
        
        KUID contactId = node.getNodeID();
        SocketAddress addr = node.getContactAddress();
        
        MessageHelper2 messageHelper = context.getMessageHelper();
        FindValueRequest request = messageHelper.createFindValueRequest(
                addr, primaryKey, Collections.singleton(secondaryKey), valueType);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("start looking for: " + request);
        }
        
        long adaptiveTimeout = node.getAdaptativeTimeout(timeout, unit);
        
        send(contactId, addr, request, adaptiveTimeout, unit);
    }

    @Override
    protected void processResponse(RequestMessage request, 
            ResponseMessage message, long time, TimeUnit unit) {
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received response: " + message);
        }
        
        // Imagine the following case: We do a lookup for a value 
        // on the 59th minute and start retrieving the values on 
        // the 60th minute. As values expire on the 60th minute 
        // it may no longer exists and the remote Node returns us
        // a Set of the k-closest Nodes instead.
        if (!(message instanceof FindValueResponse)) {
            setException(new FileNotFoundException());
            return;
        }
        
        FindValueResponse response = (FindValueResponse)message;
        
        // Make sure the DHTValueEntities have the expected
        // value type.
        Collection<? extends DHTValueEntity> entities 
            = DatabaseUtils.filter(lookupKey.getDHTValueType(), 
                    response.getDHTValueEntities());
        
        Collection<EntityKey> entityKeys = Collections.emptySet();
        
        ValueEntity entity = new DefaultValueEntity(
                lookupKey, entities, entityKeys, time, unit);
        setValue(entity);
    }
    
    @Override
    protected void processTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time, TimeUnit unit) {
        setException(new FileNotFoundException());
    }
}
