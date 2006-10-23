package com.limegroup.gnutella.dht.messages;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map.Entry;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.messages.MessageFormatException;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.StatsRequest;
import com.limegroup.mojito.messages.StatsResponse;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;
import com.limegroup.mojito.messages.StatsRequest.StatisticType;
import com.limegroup.mojito.messages.StoreResponse.Status;
import com.limegroup.mojito.routing.Contact;

/**
 * MessageFactoryWire takes a true MessageFactory as an argument, 
 * delegates all requests to it and wraps the costructed instances of
 * DHTMessage into "Wire" Messages.
 */
public class MessageFactoryWire implements MessageFactory {
    
    private MessageFactory delegate;

    public MessageFactoryWire(MessageFactory delegate) {
        if (delegate == null) {
            throw new NullPointerException("Delegate is null");
        } else if (delegate instanceof MessageFactoryWire) {
            throw new IllegalArgumentException("Recursive delegation");
        }
        
        this.delegate = delegate;
    }
    
    public DHTMessage createMessage(SocketAddress src, ByteBuffer... data) 
            throws MessageFormatException, IOException {
        
        DHTMessage msg = delegate.createMessage(src, data);
        
        if (msg instanceof PingRequest) {
            return new PingRequestWireImpl((PingRequest)msg);
        } else if (msg instanceof PingResponse) {
            return new PingResponseWireImpl((PingResponse)msg);
        } else if (msg instanceof FindNodeRequest) {
            return new FindNodeRequestWireImpl((FindNodeRequest)msg);
        } else if (msg instanceof FindNodeResponse) {
            return new FindNodeResponseWireImpl((FindNodeResponse)msg);
        } else if (msg instanceof FindValueRequest) {
            return new FindValueRequestWireImpl((FindValueRequest)msg);
        } else if (msg instanceof FindValueResponse) {
            return new FindValueResponseWireImpl((FindValueResponse)msg);
        } else if (msg instanceof StoreRequest) {
            return new StoreRequestWireImpl((StoreRequest)msg);
        } else if (msg instanceof StoreResponse) {
            return new StoreResponseWireImpl((StoreResponse)msg);
        } else if (msg instanceof StatsRequest) {
            return new StatsRequestWireImpl((StatsRequest)msg);
        } else if (msg instanceof StatsResponse) {
            return new StatsResponseWireImpl((StatsResponse)msg);
        }
        
        throw new IOException(msg.getClass() + " is unhandled");
    }
    
    public ByteBuffer writeMessage(SocketAddress dst, DHTMessage message) throws IOException {
        return delegate.writeMessage(dst, message);
    }
    
    public FindNodeRequest createFindNodeRequest(Contact contact, MessageID messageId, KUID lookupId) {
        return new FindNodeRequestWireImpl(
                delegate.createFindNodeRequest(contact, messageId, lookupId));
    }

    public FindNodeResponse createFindNodeResponse(Contact contact, MessageID messageId, 
            QueryKey queryKey, Collection<? extends Contact> nodes) {
        return new FindNodeResponseWireImpl(
                delegate.createFindNodeResponse(contact, messageId, queryKey, nodes));
    }

    public FindValueRequest createFindValueRequest(Contact contact, MessageID messageId, 
            KUID lookupId, Collection<KUID> keys) {
        return new FindValueRequestWireImpl(
                delegate.createFindValueRequest(contact, messageId, lookupId, keys));
    }

    public FindValueResponse createFindValueResponse(Contact contact, MessageID messageId, 
            Collection<KUID> keys, Collection<? extends DHTValue> values) {
        return new FindValueResponseWireImpl(
                delegate.createFindValueResponse(contact, messageId, keys, values));
    }

    public PingRequest createPingRequest(Contact contact, MessageID messageId) {
        return new PingRequestWireImpl(
                delegate.createPingRequest(contact, messageId));
    }

    public PingResponse createPingResponse(Contact contact, MessageID messageId, 
            SocketAddress externalAddress, BigInteger estimatedSize) {
        return new PingResponseWireImpl(
                delegate.createPingResponse(contact, messageId, externalAddress, estimatedSize));
    }

    public StatsRequest createStatsRequest(Contact contact, MessageID messageId, StatisticType stats) {
        return new StatsRequestWireImpl(
                delegate.createStatsRequest(contact, messageId, stats));
    }

    public StatsResponse createStatsResponse(Contact contact, MessageID messageId, String statistics) {
        return new StatsResponseWireImpl(
                delegate.createStatsResponse(contact, messageId, statistics));
    }

    public StoreRequest createStoreRequest(Contact contact, MessageID messageId, 
            QueryKey queryKey, Collection<? extends DHTValue> values) {
        return new StoreRequestWireImpl(
                delegate.createStoreRequest(contact, messageId, queryKey, values));
    }

    public StoreResponse createStoreResponse(Contact contact, MessageID messageId, 
            Collection<? extends Entry<KUID, Status>> status) {
        return new StoreResponseWireImpl(
                delegate.createStoreResponse(contact, messageId, status));
    }
}
