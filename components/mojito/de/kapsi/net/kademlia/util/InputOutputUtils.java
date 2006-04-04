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
 
package de.kapsi.net.kademlia.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.kapsi.net.kademlia.io.MessageInputStream;
import de.kapsi.net.kademlia.io.MessageOutputStream;
import de.kapsi.net.kademlia.messages.Message;

public final class InputOutputUtils {

   private InputOutputUtils() {}
   
   private static final String ALGORITHM = "MD5";
   
   public static byte[] serialize(Message message) throws IOException {
       ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
       GZIPOutputStream gz = new GZIPOutputStream(baos);
       MessageOutputStream out = new MessageOutputStream(gz);
       out.write(message);
       out.close();
       //return sign(baos.toByteArray()); // GZIP's CRC32 should be safe
       return baos.toByteArray();
   }
   
   public static Message deserialize(byte[] data) throws IOException {
       //ByteArrayInputStream bais = new ByteArrayInputStream(verify(data)); // see serialize
       ByteArrayInputStream bais = new ByteArrayInputStream(data);
       GZIPInputStream gz = new GZIPInputStream(bais);
       MessageInputStream in = new MessageInputStream(gz);
       Message message = in.readMessage();
       in.close();
       return message;
   }
   
   private static byte[] sign(byte[] data) throws IOException {
       try {
           byte[] digest = MessageDigest.getInstance(ALGORITHM).digest(data);
           byte[] result = new byte[data.length + digest.length];
           System.arraycopy(digest, 0, result, 0, digest.length);
           System.arraycopy(data, 0, result, digest.length, data.length);
           return result;
       } catch (NoSuchAlgorithmException err) {
           throw new IOException(err.getMessage());
       }
   }
   
   private static byte[] verify(byte[] data) throws IOException {
       try {
           MessageDigest md = MessageDigest.getInstance(ALGORITHM);
           final int digestLength = md.getDigestLength();
           md.update(data, digestLength, data.length-digestLength);
           byte[] digest = md.digest();
           
           for(int i = 0; i < digest.length; i++) {
               if (digest[i] != data[i]) {
                   throw new IOException("Parity check failed");
               }
           }
           
           byte[] result = new byte[data.length - digestLength];
           System.arraycopy(data, digestLength, result, 0, result.length);
           return result;
       } catch (NoSuchAlgorithmException err) {
           throw new IOException(err.getMessage());
       }
   }
}
