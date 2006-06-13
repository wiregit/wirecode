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

package com.limegroup.mojito.messages.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.messages.Message;

/**
 * An abstract class that inherits from CORE's Message and its
 * primary function is to give us the ability to interact with
 * the CORE (if Java had multiple inheritance we wouldn't need this).
 * 
 * See AbstractDHTMessage for how to remove this dependence if
 * you're not planning to send Messages through the CORE! 
 */
public abstract class AbstractMessage extends Message {
    
    public static final byte F_DHT_MESSAGE = (byte)0x43;
    
    private static final byte TTL = (byte)0x01;
    private static final byte HOPS = (byte)0x00;
    
    private byte[] payload;
    
    AbstractMessage() {
        super(makeGuid(), F_DHT_MESSAGE, TTL, HOPS, 0, N_UNKNOWN);
    }

    public void recordDrop() {
    }

    public Message stripExtendedPayload() {
        return this;
    }

    public void write(OutputStream out) throws IOException {
        if (getLength() == 0) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(640);
            writeMessage(baos);
            baos.close();
            
            payload = baos.toByteArray();
            updateLength(payload.length);
        }
        
        super.write(out);
    }

    protected void writePayload(OutputStream out) throws IOException {
        out.write(payload, 0, payload.length);
    }
    
    protected abstract void writeMessage(OutputStream out) throws IOException;
}
