package org.limewire.mojito.handler.response2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent2.AsyncFuture;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.entity.DefaultValueEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.messages.FindValueRequest;
import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito.messages.MessageHelper;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.DatabaseUtils;

public class GetValueResponseHandler extends AbstractResponseHandler<ValueEntity> {

    private static final Log LOG 
        = LogFactory.getLog(GetValueResponseHandler.class);
    
    private final EntityKey lookupKey;
    
    public GetValueResponseHandler(Context context, 
            EntityKey lookupKey) {
        super(context);
        
        this.lookupKey = lookupKey;
    }

    @Override
    protected void doStart(AsyncFuture<ValueEntity> future) {
        Contact node = lookupKey.getContact();
        KUID primaryKey = lookupKey.getPrimaryKey();
        KUID secondaryKey = lookupKey.getSecondaryKey();
        DHTValueType valueType = lookupKey.getDHTValueType();
        
        MessageHelper messageHelper = context.getMessageHelper();
        FindValueRequest request = messageHelper.createFindValueRequest(
                node.getContactAddress(), primaryKey, 
                Collections.singleton(secondaryKey), valueType);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("start looking for: " + request);
        }
        
        MessageDispatcher messageDispatcher = context.getMessageDispatcher();
        messageDispatcher.send(node, request, this);
    }

    @Override
    protected void processResponse(ResponseMessage message, 
            long time, TimeUnit unit) {
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Eeceived response: " + message);
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
    }
    
    @Override
    protected void processError(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e) {
    }
}
