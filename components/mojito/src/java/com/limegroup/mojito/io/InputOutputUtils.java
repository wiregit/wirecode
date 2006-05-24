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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.limegroup.mojito.messages.DHTMessage;


public final class InputOutputUtils {

    private static final boolean COMPRESS = true;
    
    private InputOutputUtils() {
    }

    public static byte[] serialize(DHTMessage message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        
        MessageOutputStream out = null;
        if (COMPRESS) {
            GZIPOutputStream gz = new GZIPOutputStream(baos);
            out = new MessageOutputStream(gz);
        } else {
            out = new MessageOutputStream(baos);
        }
        
        out.write(message);
        out.close();
        return baos.toByteArray();
    }

    public static DHTMessage deserialize(SocketAddress src, byte[] data)
            throws MessageFormatException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            
            MessageInputStream in = null;
            if (COMPRESS) {
                GZIPInputStream gz = new GZIPInputStream(bais);
                in = new MessageInputStream(gz);
            } else {
                in = new MessageInputStream(bais);
            }
            
            DHTMessage message = in.readMessage(src);
            in.close();
            return message;
        } catch (IOException e) {
            throw new MessageFormatException(src + " sent a malformed message", e);
        }
    }
}
