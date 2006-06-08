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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.MessageFactory;


public final class InputOutputUtils {

    private InputOutputUtils() {
    }

    public static ByteBuffer serialize(DHTMessage message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
        MessageOutputStream out = new MessageOutputStream(baos);
        
        out.write(message);
        out.close();
        return ByteBuffer.wrap(baos.toByteArray());
    }

    public static DHTMessage deserialize(MessageFactory factory, 
            SocketAddress src, ByteBuffer data)
                throws MessageFormatException {
        
        try {
            ByteBufferInputStream bbis = new ByteBufferInputStream(data);
            MessageInputStream in = new MessageInputStream(bbis, factory, src);
            
            DHTMessage message = in.readMessage();
            in.close();
            return message;
        } catch (IOException e) {
            throw new MessageFormatException(src + " sent a malformed message", e);
        }
    }
}
