package org.limewire.mojito.io;

import java.net.SocketAddress;

import org.limewire.mojito.KUID;
import org.limewire.mojito.message.MessageID;
import org.limewire.mojito.message.NodeRequest;
import org.limewire.mojito.message.NodeResponse;
import org.limewire.mojito.message.PingRequest;
import org.limewire.mojito.message.PingResponse;
import org.limewire.mojito.message.RequestMessage;
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.message.StoreRequest;
import org.limewire.mojito.message.StoreResponse;
import org.limewire.mojito.message.ValueRequest;
import org.limewire.mojito.message.ValueResponse;
import org.limewire.mojito.routing.Contact;

public class RequestHandle {

    private final KUID contactId;
    
    private final SocketAddress address;
    
    private final RequestMessage request;
    
    public RequestHandle(KUID contactId, 
            SocketAddress address, RequestMessage request) {
        
        this.contactId = contactId;
        this.address = address;
        this.request = request;
    }

    public KUID getContactId() {
        return contactId;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public RequestMessage getRequest() {
        return request;
    }
    
    MessageID getMessageId() {
        return request.getMessageId();
    }
    
    /**
     * 
     */
    boolean check(ResponseMessage response) {
        return checkType(response) && checkContactId(response);
    }
    
    /**
     * 
     */
    private boolean checkType(ResponseMessage response) {
        if (request instanceof PingRequest) {
            return response instanceof PingResponse;
        } else if (request instanceof NodeRequest) {
            return response instanceof NodeResponse;
        } else if (request instanceof ValueRequest) {
            return response instanceof ValueResponse 
                || response instanceof NodeResponse;
        } else if (request instanceof StoreRequest) {
            return response instanceof StoreResponse;
        }
        
        return false;
    }
    
    /**
     * 
     */
    private boolean checkContactId(ResponseMessage response) {
        if (contactId == null) {
            return (request instanceof PingRequest);
        }
        
        Contact contact = response.getContact();
        KUID otherId = contact.getContactId();
        
        return contactId.equals(otherId);
    }
}
