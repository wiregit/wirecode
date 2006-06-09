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

package com.limegroup.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.messages.DHTMessage;

/**
 * An implementation of MessageDispatcher for debugging purposes. 
 * It allows us to read/write Messages in LimeDHTMessage format 
 * which means you can start an instance of the LimeWire core and 
 * an arbitary number of DHT Nodes that are using this implementation.
 */
public class LimeStandaloneMessageDispatcherImpl 
        extends MessageDispatcherImpl {

    private static final int MESSAGE_HEADER = 23;
    
    private Random random = new Random();
    
    public LimeStandaloneMessageDispatcherImpl(Context context) {
        super(context);
    }

    protected ByteBuffer serialize(DHTMessage message) 
            throws IOException {
        ByteBuffer payload = super.serialize(message);
        
        ByteBuffer data = ByteBuffer.allocate(2048);
        data.order(ByteOrder.LITTLE_ENDIAN);
        
        // GUID 16 bytes (4*4 byte)
        for(int i = 0; i < 4; i++) {
            data.putInt(random.nextInt());
        }
        
        data.put((byte)0x43); // LimeDHTMessage
        data.put((byte)0x01); // TTL
        data.put((byte)0x00); // Hops
        
        data.putInt(payload.remaining()); // Length
        
        // Must be 23 byte total
        assert data.position() == MESSAGE_HEADER;
        
        data.order(ByteOrder.BIG_ENDIAN);
        data.put(payload);
        
        data.flip();
        return data;
    }

    protected DHTMessage deserialize(SocketAddress src, ByteBuffer data) 
            throws MessageFormatException, IOException {
        
        try {
            // Skip the Gnutella Header
            data.position(MESSAGE_HEADER);
        } catch (IllegalArgumentException err) {
            throw new MessageFormatException(err);
        }
        
        return super.deserialize(src, data);
    }
}
