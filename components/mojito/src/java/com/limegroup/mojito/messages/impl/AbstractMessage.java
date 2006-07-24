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

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.mojito.messages.DHTMessage;

/**
 * An abstract class that inherits from CORE's Message and its
 * primary function is to give us the ability to interact with
 * the CORE (if Java had multiple inheritance we wouldn't need this).
 * 
 * See AbstractDHTMessage for how to remove this dependence if
 * you're not planning to send Messages through the CORE! 
 */
abstract class AbstractMessage extends Message {
    
    /** 
     * An empty GUID, it's never written to Network.
     * See overwritten write-methods for more info!
     */
    private static final byte[] GUID = new byte[16];
    
    /** Default TTL */
    private static final byte TTL = (byte)0x01;
    
    /** Default HOPS */
    private static final byte HOPS = (byte)0x00;
    
    AbstractMessage() {
        super(GUID, DHTMessage.F_DHT_MESSAGE, TTL, HOPS, 0, N_UNKNOWN);
    }

    public void recordDrop() {
    }

    public Message stripExtendedPayload() {
        return this;
    }

    @Override
    public abstract void write(OutputStream out) throws IOException;
    
    @Override
    public final void write(OutputStream out, byte[] buf) throws IOException {
        write(out);
    }

    @Override
    public final void writeQuickly(OutputStream out) throws IOException {
        write(out);
    }
    
    @Override
    protected final void writePayload(OutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }
}
