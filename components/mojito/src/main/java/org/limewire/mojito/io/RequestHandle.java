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

/**
 * 
 */
public class RequestHandle {

    private final KUID contactId;
    
    private final SocketAddress address;
    
    private final RequestMessage request;
    
    public RequestHandle(KUID contactId, 
            SocketAddress address, 
            RequestMessage request) {
        
        this.contactId = contactId;
        this.address = address;
        this.request = request;
    }

    /**
     * Returns the remote host's {@link KUID}.
     */
    public KUID getContactId() {
        return contactId;
    }

    /**
     * Returns the remote host's {@link SocketAddress}.
     */
    public SocketAddress getAddress() {
        return address;
    }

    /**
     * Returns the {@link RequestMessage} that was sent to the remote host.
     */
    public RequestMessage getRequest() {
        return request;
    }
    
    /**
     * Returns the {@link MessageID} of the {@link RequestMessage}.
     */
    MessageID getMessageId() {
        return request.getMessageId();
    }
    
    /**
     * Returns {@code true} if the given {@link ResponseMessage} 
     * fulfills all requirements.
     */
    boolean check(ResponseMessage response) {
        return checkType(response) && checkContactId(response);
    }
    
    /**
     * Returns {@code true} if the {@link ResponseMessage} has 
     * the correct type.
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
     * Returns {@code true} if the {@link KUID}s match.
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
