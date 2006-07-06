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
 
package com.limegroup.mojito.messages.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.ByteOrder;
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
    
    /** The function ID of our DHT Message */
    public static final byte F_DHT_MESSAGE = (byte)0x43;
    
    /** GUID goes from 0 to 16th byte */
    public static final int GUID_END = 16;
    
    /** PAYLOAD starts at 23rd byte */
    public static final int PAYLOAD_START = 23;
    
    /** 
     * An empty GUID, it's never written to Network.
     * See overwritten write-methods for more info!
     */
    private static final byte[] GUID = new byte[16];
    
    /** Default TTL */
    private static final byte TTL = (byte)0x01;
    
    /** Default HOPS */
    private static final byte HOPS = (byte)0x00;
    
    /** The serialized  Message */
    private byte[] payload;
    
    AbstractMessage() {
        super(GUID, F_DHT_MESSAGE, TTL, HOPS, 0, N_UNKNOWN);
    }

    public void recordDrop() {
    }

    public Message stripExtendedPayload() {
        return this;
    }

    /**
     * Serialized this Message if it has not been serialized yet.
     */
    private void serialize() throws IOException {
        if (getLength() == 0) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(640);
            writePayload(baos);
            baos.close();
            
            payload = baos.toByteArray();
            updateLength(payload.length);
        }
    }
    
    @Override
    public void writeQuickly(OutputStream out) throws IOException {
        serialize();
        
        out.write(payload, 0, 16);
        out.write(F_DHT_MESSAGE);
        out.write(TTL);
        out.write(HOPS);
        ByteOrder.int2leb(payload.length-16, out);
        out.write(payload, 16, payload.length-16);
    }
    
    @Override
    public void write(OutputStream out, byte[] buf) throws IOException {
        serialize();
        
        System.arraycopy(payload, 0, buf, 0, 16);
        buf[16] = F_DHT_MESSAGE;
        buf[17] = TTL;
        buf[18] = HOPS;
        ByteOrder.int2leb(payload.length-16, buf, 19);
        
        out.write(buf);
        out.write(payload, 16, payload.length-16);
    }
    
    protected abstract void writePayload(OutputStream out) throws IOException;
}
