package org.limewire.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.entity.DefaultSecurityTokenEntity;
import org.limewire.mojito.entity.SecurityTokenEntity;
import org.limewire.mojito.message.MessageHelper;
import org.limewire.mojito.message.NodeRequest;
import org.limewire.mojito.message.NodeResponse;
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

/**
 * An implementation of {@link ResponseHandler} to retrieve a
 * node's {@link SecurityToken}.
 */
public class SecurityTokenResponseHandler 
        extends AbstractResponseHandler<SecurityTokenEntity> {

    private final Contact contact;
    
    private final KUID lookupId;
    
    public SecurityTokenResponseHandler(Context context, 
            Contact contact, KUID lookupId, 
            long timeout, TimeUnit unit) {
        super(context, timeout, unit);
        
        this.contact = contact;
        this.lookupId = lookupId;
    }

    @Override
    protected void start() throws IOException {
        KUID contactId = contact.getContactId();
        SocketAddress addr = contact.getContactAddress();
        
        MessageHelper messageHelper = context.getMessageHelper();
        NodeRequest request = messageHelper.createFindNodeRequest(addr, lookupId);
        
        long adaptiveTimeout = contact.getAdaptativeTimeout(timeout, unit);
        send(contactId, addr, request, adaptiveTimeout, unit);
    }
    
    @Override
    protected void processResponse(RequestHandle request, 
            ResponseMessage response, long time,
            TimeUnit unit) throws IOException {
        
        NodeResponse nr = (NodeResponse)response;
        
        Contact src = nr.getContact();
        SecurityToken securityToken = nr.getSecurityToken();
        
        if (securityToken != null) {
            setValue(new DefaultSecurityTokenEntity(
                    src, securityToken, time, unit));
        } else {
            setException(new IllegalStateException());
        }
    }
}
