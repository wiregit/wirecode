package org.limewire.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.Context2;
import org.limewire.mojito.KUID;
import org.limewire.mojito.entity.DefaultNodeEntity;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.message2.MessageHelper2;
import org.limewire.mojito.message2.NodeRequest;
import org.limewire.mojito.message2.NodeResponse;
import org.limewire.mojito.message2.RequestMessage;
import org.limewire.mojito.message2.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

public class NodeResponseHandler2 extends LookupResponseHandler2<NodeEntity> {

    public NodeResponseHandler2(Context2 context, 
            KUID lookupId, 
            long timeout, TimeUnit unit) {
        super(context, lookupId, timeout, unit);
    }
    
    public NodeResponseHandler2(Context2 context, 
            KUID lookupId, Contact[] contacts, 
            long timeout, TimeUnit unit) {
        super(context, lookupId, contacts, timeout, unit);
    }
    
    @Override
    protected void complete(State state) {
        Entry<Contact, SecurityToken>[] contacts 
            = state.getContacts();
        
        if (contacts.length == 0) {
            setException(new NoSuchNodeException(state));
        } else {
            setValue(new DefaultNodeEntity(state));
        }
    }

    @Override
    protected void lookup(Contact dst, KUID lookupId, 
            long timeout, TimeUnit unit) throws IOException {
        
        KUID contactId = dst.getNodeID();
        SocketAddress addr = dst.getContactAddress();
        
        MessageHelper2 messageHelper = context.getMessageHelper();
        NodeRequest request = messageHelper.createFindNodeRequest(addr, lookupId);
        
        long adaptiveTimeout = dst.getAdaptativeTimeout(timeout, unit);
        send(contactId, addr, request, adaptiveTimeout, unit);
    }

    @Override
    protected void processResponse0(RequestMessage request,
            ResponseMessage message, long time, TimeUnit unit) throws IOException {
        
        NodeResponse response = (NodeResponse)message;
        
        Contact src = message.getContact();
        SecurityToken securityToken = response.getSecurityToken();
        
        Contact[] contacts = response.getContacts();
        processContacts(src, securityToken, contacts, time, unit);
    }
    
    private static class NoSuchNodeException extends IOException {
        
        private final State state;
        
        public NoSuchNodeException(State state) {
            this.state = state;
        }
        
        public State getState() {
            return state;
        }
    }
}
