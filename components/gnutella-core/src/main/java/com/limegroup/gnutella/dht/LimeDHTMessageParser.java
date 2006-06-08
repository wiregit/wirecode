package com.limegroup.gnutella.dht;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;
import com.limegroup.mojito.io.MessageInputStream;

class LimeDHTMessageParser implements MessageParser {
    
    private static final SocketAddress ADDRESS = new InetSocketAddress(0);
    
    private LimeDHTMessageFactory factory;
    
    LimeDHTMessageParser(LimeDHTMessageFactory factory) {
        this.factory = factory;
    }
    
    public Message parse(byte[] guid, byte ttl, byte hops, 
            byte[] payload, int network) throws BadPacketException {
        
        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        MessageInputStream in = new MessageInputStream(bais, factory, ADDRESS);
        try {
            return (LimeDHTMessage2)in.readMessage();
        } catch (IOException err) {
            throw new BadPacketException(err);
        } finally {
            try { in.close(); } catch (IOException ignore) {}
        }
        //return new LimeDHTMessage(guid, ttl, hops, payload, network);
    }
}
