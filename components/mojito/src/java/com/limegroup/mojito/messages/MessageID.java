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

package com.limegroup.mojito.messages;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Random;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.util.ArrayUtils;

/**
 * A pseudo random Message Identifier
 * 
 * This class is immutable!
 */
public class MessageID implements Comparable<MessageID>, Serializable {

    private static final long serialVersionUID = -1477232241287654597L;

    public static final int LENGTH = 16;

    private static final Random GENERATOR = new Random();

    /**
     * A random pad we're using to obfuscate the actual
     * QueryKey. Nodes must do a lookup to get the QK!
     */
    private static final byte[] RANDOM_PAD = new byte[4];

    static {
        GENERATOR.nextBytes(RANDOM_PAD);
    }

    private byte[] messageId;

    private int hashCode = -1;

    private MessageID(byte[] messageId) {
        if (messageId == null) {
            throw new NullPointerException("messageId cannot be null");
        }
        
        if (messageId.length != LENGTH) {
            throw new IllegalArgumentException("MessageID must be " 
                    + LENGTH + " bytes long: " + messageId.length);
        }
        
        this.messageId = messageId;
    }

    /**
     * Creates and returns a MessageID from the given byte Array
     */
    public static MessageID create(byte[] messageId) {
        return new MessageID(messageId);
    }

    /**
     * Creates a pseudo random MessageID and tags it with the given 
     * SocketAddress. 
     */
    public static MessageID create(SocketAddress dst) {
        byte[] messageId = new byte[LENGTH];
        GENERATOR.nextBytes(messageId);

        if (dst instanceof InetSocketAddress) {
            byte[] queryKey = QueryKey.getQueryKey(dst).getBytes();

            // Obfuscate it with our random pad!
            for(int i = 0; i < RANDOM_PAD.length; i++) {
                messageId[i] = (byte)(queryKey[i] ^ RANDOM_PAD[i]);
            }
        }

        return create(messageId);
    }

    /**
     * Creates and returns a MessageID from the given
     * hex encoded String
     */
    public static MessageID create(String id) {
        return create(ArrayUtils.parseHexString(id));
    }
    
    /**
     * Writes this MessageID to the OutputStream
     */
    public void write(OutputStream os) throws IOException {
        os.write(messageId, 0, messageId.length);
    }

    /**
     * Extracts and returns the QueryKey from the GUID
     */
    private QueryKey getQueryKey() {
        byte[] queryKey = new byte[4];

        // De-Obfuscate it with our random pad!
        for(int i = 0; i < RANDOM_PAD.length; i++) {
            queryKey[i] = (byte)(messageId[i] ^ RANDOM_PAD[i]);
        }

        return QueryKey.getQueryKey(queryKey, true);
    }

    /**
     * Returns whether or not we're the originator of the GUID.
     */
    public boolean verifyQueryKey(SocketAddress src) {
        if (!(src instanceof InetSocketAddress)) {
            return false;
        }

        return getQueryKey().isFor(src);
    }

    public int compareTo(MessageID o) {
        int d = 0;
        for(int i = 0; i < messageId.length; i++) {
            d = (messageId[i] & 0xFF) - (o.messageId[i] & 0xFF);
            if (d < 0) {
                return -1;
            } else if (d > 0) {
                return 1;
            }
        }

        return 0;
    }

    public int hashCode() {
        if (hashCode == -1) {
            hashCode = Arrays.hashCode(messageId);
            if (hashCode == -1) {
                hashCode = 0;
            }
        }

        return hashCode;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof MessageID)) {
            return false;
        }

        return Arrays.equals(messageId, ((MessageID)o).messageId);
    }

    /**
     * Returns this MessageID as a hex String
     */
    public String toHexString() {
        return ArrayUtils.toHexString(messageId);
    }

    /**
     * Returns this MessageID as a binary String
     */
    public String toBinString() {
        return ArrayUtils.toBinString(messageId);
    }

    public String toString() {
        return "MessageID: " + toHexString();
    }
}
