package org.limewire.mojito.message;

import java.math.BigInteger;
import java.net.SocketAddress;

import org.limewire.mojito.routing.Contact;

/**
 * The default implementation of a {@link PingResponse}.
 */
public class DefaultPingResponse extends AbstractResponse 
        implements PingResponse {

    private final SocketAddress externalAddress;
    
    private final BigInteger estimatedSize;
    
    public DefaultPingResponse(MessageID messageId, Contact contact, 
            Contact dst, BigInteger estimatedSize) {
        this(messageId, contact, dst.getSourceAddress(), estimatedSize);
    }
    
    public DefaultPingResponse(MessageID messageId, Contact contact, 
            SocketAddress externalAddress, BigInteger estimatedSize) {
        super(messageId, contact);
        
        this.externalAddress = externalAddress;
        this.estimatedSize = estimatedSize;
    }

    @Override
    public BigInteger getEstimatedSize() {
        return estimatedSize;
    }

    @Override
    public SocketAddress getExternalAddress() {
        return externalAddress;
    }
}
