package org.limewire.mojito2.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.util.DatabaseUtils;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.entity.DefaultValueEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.message.MessageHelper;
import org.limewire.mojito2.message.RequestMessage;
import org.limewire.mojito2.message.ResponseMessage;
import org.limewire.mojito2.message.ValueRequest;
import org.limewire.mojito2.message.ValueResponse;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueType;

public class GetValueResponseHandler extends AbstractResponseHandler<ValueEntity> {

    private static final Log LOG 
        = LogFactory.getLog(GetValueResponseHandler.class);
    
    private final EntityKey lookupKey;
    
    public GetValueResponseHandler(Context context, 
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
        
        MessageHelper messageHelper = context.getMessageHelper();
        ValueRequest request = messageHelper.createFindValueRequest(
                addr, primaryKey, new KUID[] { secondaryKey }, valueType);
        
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
        if (!(message instanceof ValueResponse)) {
            setException(new FileNotFoundException());
            return;
        }
        
        ValueResponse response = (ValueResponse)message;
        
        // Make sure the DHTValueEntities have the expected
        // value type.
        DHTValueEntity[] entities 
            = DatabaseUtils.filter(lookupKey.getDHTValueType(), 
                    response.getValueEntities());
        
        EntityKey[] entityKeys = new EntityKey[0];
        
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
