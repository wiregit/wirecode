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
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageInputStream;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.PingResponse;

/**
 * An implementation of PingResponse (Pong)
 */
public class PingResponseImpl extends AbstractResponseMessage
        implements PingResponse {

    private SocketAddress externalAddress;
    private int estimatedSize;

    public PingResponseImpl(Context context, 
	    Contact contact, KUID messageId, 
	    SocketAddress externalAddress, int estimatedSize) {
        super(context, OpCode.PING_RESPONSE, contact, messageId);

        this.externalAddress = externalAddress;
        this.estimatedSize = estimatedSize;
    }

    public PingResponseImpl(Context context, 
            SocketAddress src, ByteBuffer... data) throws IOException {
        super(context, OpCode.PING_RESPONSE, src, data);
        
        MessageInputStream in = getMessageInputStream();
        
        this.externalAddress = in.readSocketAddress();
        this.estimatedSize = in.readInt();
    }
    
    /** My external address */
    public SocketAddress getExternalAddress() {
        return externalAddress;
    }

    public int getEstimatedSize() {
        return estimatedSize;
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeSocketAddress(externalAddress);
        out.writeInt(estimatedSize);
    }

    public String toString() {
        return "PingResponse: externalAddress=" + externalAddress + ", estimatedSize=" + estimatedSize;
    }
}
