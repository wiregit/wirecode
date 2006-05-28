/*
 * Mojito Distributed Hash Tabe (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;
import com.limegroup.mojito.io.InputOutputUtils;
import com.limegroup.mojito.io.MessageFormatException;
import com.limegroup.mojito.messages.DHTMessage;

/**
 * LimeDHTMessage is a wrapper class for Mojito DHTMessage(s). This 
 * allows us to route the Mojito DHTMessage(s) through the LimeWire core.
 * The payload is a serialized DHTMessage.
 */
public class LimeDHTMessage extends Message {

    private static final long serialVersionUID = 3867749049179828044L;
    
    private static final byte F_DHT_MESSAGE = (byte)0x43;
    
    static void registerMessage() {
        MessageFactory.setParser(F_DHT_MESSAGE, new LimeDHTMessageParser());
    }
    
    private byte[] payload;
    
    private LimeDHTMessage(byte[] payload) {
        super(makeGuid(), F_DHT_MESSAGE, (byte)0x01, (byte)0x00, payload.length, N_UNKNOWN);
        this.payload = payload;
    }
    
    private LimeDHTMessage(byte[] guid, byte ttl, byte hops, byte[] payload, int network) {
        super(guid, F_DHT_MESSAGE, ttl, hops, payload.length, network);
        this.payload = payload;
    }

    public static LimeDHTMessage createMessage(DHTMessage msg) throws BadPacketException, IOException {
        ByteBuffer payload = InputOutputUtils.serialize(msg);
        return createMessage(payload.array());
    }

    public static LimeDHTMessage createMessage(byte[] payload) throws BadPacketException, IOException {
        return new LimeDHTMessage(payload);
    }
    
    protected void writePayload(OutputStream out) throws IOException {
        out.write(payload);
    }
    
    public void recordDrop() {
    }

    public Message stripExtendedPayload() {
        return this;
    }
    
    public DHTMessage getDHTMessage(SocketAddress src) throws MessageFormatException {
        return InputOutputUtils.deserialize(src, ByteBuffer.wrap(payload));
    }
    
    /**
     * An implementation of MessagePareser to parse LimeDHTMessage(s)
     */
    private static class LimeDHTMessageParser implements MessageParser {
        public Message parse(byte[] guid, byte ttl, byte hops, 
                byte[] payload, int network) throws BadPacketException {
            
            return new LimeDHTMessage(guid, ttl, hops, payload, network);
        }
    }
}
