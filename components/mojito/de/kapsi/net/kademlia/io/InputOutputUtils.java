/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package de.kapsi.net.kademlia.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.kapsi.net.kademlia.messages.Message;

public final class InputOutputUtils {

   private InputOutputUtils() {}
   
   public static byte[] serialize(Message message) throws IOException {
       ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
       GZIPOutputStream gz = new GZIPOutputStream(baos);
       MessageOutputStream out = new MessageOutputStream(gz);
       out.write(message);
       out.close();
       return baos.toByteArray();
   }
   
   public static Message deserialize(byte[] data) throws MessageFormatException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            GZIPInputStream gz = new GZIPInputStream(bais);
            MessageInputStream in = new MessageInputStream(gz);
            Message message = in.readMessage();
            in.close();
            return message;
        } catch (IOException e) {
            throw new MessageFormatException(e.getMessage());
        }
    }
}
