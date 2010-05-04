package org.limewire.mojito2.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.Context;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.entity.DefaultSecurityTokenEntity;
import org.limewire.mojito2.entity.SecurityTokenEntity;
import org.limewire.mojito2.message.MessageHelper;
import org.limewire.mojito2.message.NodeRequest;
import org.limewire.mojito2.message.NodeResponse;
import org.limewire.mojito2.message.ResponseMessage;
import org.limewire.mojito2.routing.Contact;
import org.limewire.security.SecurityToken;

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
        KUID contactId = contact.getNodeID();
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
