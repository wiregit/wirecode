package com.limegroup.gnutella.rudp;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.MessageFormatException;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;

public class LimeRUDPMessageParser implements MessageParser {
    
    /** The factory creating the messages. */
    private RUDPMessageFactory factory;
    
    public LimeRUDPMessageParser(RUDPMessageFactory factory) {
        this.factory = factory;
    }
    
    public Message parse(byte[] header, byte[] payload, 
            byte softMax, int network) throws BadPacketException, IOException {
        
        try {
            return (Message)factory.createMessage(null, 
                    ByteBuffer.wrap(header), 
                    ByteBuffer.wrap(payload));
        } catch(MessageFormatException mfe) {
            throw new BadPacketException(mfe);
        }
    }

}
