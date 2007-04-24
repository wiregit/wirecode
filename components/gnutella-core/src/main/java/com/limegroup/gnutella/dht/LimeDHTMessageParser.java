package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.mojito.messages.MessageFactory;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;

/**
 * The LimeDHTMessageParser class delegates parse
 * requests to Mojito's MessageFactory
 */
class LimeDHTMessageParser implements MessageParser {
    
    /**
     * A handle to Mojito's MessageFactory
     */
    private MessageFactory factory;
    
    LimeDHTMessageParser(MessageFactory factory) {
        this.factory = factory;
    }
    
    public Message parse(byte[] header, byte[] payload, 
            byte softMax, Network network) throws BadPacketException, IOException {
        
        return (Message)factory.createMessage(null, 
                ByteBuffer.wrap(header), 
                ByteBuffer.wrap(payload));
    }
}
