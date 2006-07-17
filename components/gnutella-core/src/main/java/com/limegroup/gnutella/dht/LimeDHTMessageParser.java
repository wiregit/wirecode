/*
 * Mojito Distributed Hash Table (Mojito DHT)
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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;
import com.limegroup.mojito.messages.MessageFactory;

/**
 * 
 */
class LimeDHTMessageParser implements MessageParser {
    
    private static final SocketAddress ADDRESS = new InetSocketAddress(0);
    
    private MessageFactory factory;
    
    LimeDHTMessageParser(MessageFactory factory) {
        this.factory = factory;
    }
    
    public Message parse(byte[] header, byte[] payload, 
            byte softMax, int network) throws BadPacketException, IOException {
        
        return (Message)factory.createMessage(ADDRESS, 
                ByteBuffer.wrap(header, 0, 16), 
                ByteBuffer.wrap(header, 17, 2), 
                ByteBuffer.wrap(payload));
    }
}
