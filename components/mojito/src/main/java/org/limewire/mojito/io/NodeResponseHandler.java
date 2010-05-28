package org.limewire.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.entity.DefaultNodeEntity;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.exceptions.NoSuchNodeException;
import org.limewire.mojito.message.MessageHelper;
import org.limewire.mojito.message.NodeRequest;
import org.limewire.mojito.message.NodeResponse;
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

public class NodeResponseHandler extends LookupResponseHandler<NodeEntity> {

    public NodeResponseHandler(Context context, 
            KUID lookupId, 
            long timeout, TimeUnit unit) {
        super(Type.FIND_NODE, context, lookupId, timeout, unit);
    }
    
    public NodeResponseHandler(Context context, 
            KUID lookupId, Contact[] contacts, 
            long timeout, TimeUnit unit) {
        super(Type.FIND_NODE, context, lookupId, contacts, timeout, unit);
    }
    
    @Override
    protected void complete(State state) {
        Entry<Contact, SecurityToken>[] contacts 
            = state.getContacts();
        
        if (contacts.length == 0) {
            setException(new DefaultNoSuchNodeException(state));
        } else {
            setValue(new DefaultNodeEntity(state));
        }
    }

    @Override
    protected void lookup(Contact dst, KUID lookupId, 
            long timeout, TimeUnit unit) throws IOException {
        
        KUID contactId = dst.getContactId();
        SocketAddress addr = dst.getContactAddress();
        
        MessageHelper messageHelper = context.getMessageHelper();
        NodeRequest request = messageHelper.createFindNodeRequest(addr, lookupId);
        
        long adaptiveTimeout = dst.getAdaptativeTimeout(timeout, unit);
        send(contactId, addr, request, adaptiveTimeout, unit);
    }

    @Override
    protected void processResponse0(RequestHandle request,
            ResponseMessage message, long time, TimeUnit unit) throws IOException {
        
        NodeResponse response = (NodeResponse)message;
        
        Contact src = message.getContact();
        SecurityToken securityToken = response.getSecurityToken();
        
        Contact[] contacts = response.getContacts();
        processContacts(src, securityToken, contacts, time, unit);
    }
    
    private static class DefaultNoSuchNodeException extends NoSuchNodeException {
        
        private final State state;
        
        public DefaultNoSuchNodeException(State state) {
            this.state = state;
        }
        
        public State getState() {
            return state;
        }
    }
}
