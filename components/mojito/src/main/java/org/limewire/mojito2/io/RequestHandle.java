package org.limewire.mojito2.io;

import java.net.SocketAddress;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.message.MessageID;
import org.limewire.mojito2.message.NodeRequest;
import org.limewire.mojito2.message.NodeResponse;
import org.limewire.mojito2.message.PingRequest;
import org.limewire.mojito2.message.PingResponse;
import org.limewire.mojito2.message.RequestMessage;
import org.limewire.mojito2.message.ResponseMessage;
import org.limewire.mojito2.message.StoreRequest;
import org.limewire.mojito2.message.StoreResponse;
import org.limewire.mojito2.message.ValueRequest;
import org.limewire.mojito2.message.ValueResponse;
import org.limewire.mojito2.routing.Contact;

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
        KUID otherId = contact.getNodeID();
        
        return contactId.equals(otherId);
    }
}
