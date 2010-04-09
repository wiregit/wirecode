package org.limewire.mojito.handler.response2;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.entity.DefaultNodeEntity;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.messages.FindNodeRequest;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.MessageHelper;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.messages.SecurityTokenProvider;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

public class NodeResponseHandler extends LookupResponseHandler<NodeEntity> {

    public NodeResponseHandler(Context context, KUID key) {
        super(context, key, -1L, TimeUnit.MILLISECONDS);
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
    protected void lookup(Contact dst, KUID key, 
            long timeout, TimeUnit unit) throws IOException {
        MessageHelper messageHelper = context.getMessageHelper();
        FindNodeRequest request = messageHelper.createFindNodeRequest(
                dst.getContactAddress(), key);
        
        MessageDispatcher messageDispatcher = context.getMessageDispatcher();
        messageDispatcher.send(dst, request, this, timeout, unit);
    }

    @Override
    protected void processResponse0(ResponseMessage message, 
            long time, TimeUnit unit) throws IOException {
        
        FindNodeResponse response = (FindNodeResponse)message;
        
        Contact src = message.getContact();
        SecurityToken securityToken = null;
        
        if (response instanceof SecurityTokenProvider) {
            securityToken = ((SecurityTokenProvider)message).getSecurityToken();
        }
        
        Contact[] contacts = response.getNodes().toArray(new Contact[0]);
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
