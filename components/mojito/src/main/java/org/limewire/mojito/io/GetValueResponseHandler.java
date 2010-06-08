package org.limewire.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;
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
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.message.ValueRequest;
import org.limewire.mojito.message.ValueResponse;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.DatabaseUtils;

/**
 * An utility class that retrieves a {@link ValueTuple} from
 * a known {@link Contact}. In other words this class does not
 * perform a DHT lookup.
 */
public class GetValueResponseHandler extends AbstractResponseHandler<ValueEntity> {

    private static final Log LOG 
        = LogFactory.getLog(GetValueResponseHandler.class);
    
    private final ValueKey valueKey;
    
    public GetValueResponseHandler(Context context, 
            ValueKey valueKey, long timeout, TimeUnit unit) {
        super(context, timeout, unit);
        
        // This class assumes we already know the exact location
        // of the value we're looking for.
        if (valueKey.isLookupKey()) {
            throw new IllegalArgumentException("valueKey=" + valueKey);
        }
        
        this.valueKey = valueKey;
    }

    @Override
    protected void start() throws IOException {
        Contact dst = valueKey.getContact();
        KUID primaryKey = valueKey.getPrimaryKey();
        KUID secondaryKey = valueKey.getSecondaryKey();
        ValueType valueType = valueKey.getValueType();
        
        KUID contactId = dst.getContactId();
        SocketAddress addr = dst.getContactAddress();
        
        MessageHelper messageHelper = context.getMessageHelper();
        ValueRequest request = messageHelper.createFindValueRequest(
                addr, primaryKey, new KUID[] { secondaryKey }, valueType);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("start looking for: " + request);
        }
        
        long adaptiveTimeout = dst.getAdaptativeTimeout(timeout, unit);
        send(contactId, addr, request, adaptiveTimeout, unit);
    }

    @Override
    protected void processResponse(RequestHandle request, 
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
            setException(new NoSuchValueException());
            return;
        }
        
        ValueResponse response = (ValueResponse)message;
        
        // Make sure the DHTValueEntities have the expected
        // value type.
        ValueTuple[] entities 
            = DatabaseUtils.filter(valueKey.getValueType(), 
                    response.getValues());
        
        ValueKey[] entityKeys = new ValueKey[0];
        
        ValueEntity entity = new DefaultValueEntity(
                valueKey, entities, entityKeys, time, unit);
        setValue(entity);
    }
    
    @Override
    protected void processTimeout(RequestHandle message, 
            long time, TimeUnit unit) {
        setException(new NoSuchValueException());
    }
}
