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

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.io.MessageInputStream;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.PingRequest;

/**
 * An implementation of PingRequest
 */
public class PingRequestImpl extends AbstractRequestMessage
        implements PingRequest {

    public PingRequestImpl(Context context,
            Contact contact, MessageID messageId) {
        super(context, OpCode.PING_REQUEST, contact, messageId);
    }
    
    public PingRequestImpl(Context context, 
            SocketAddress src, MessageInputStream in) throws IOException {
        super(context, OpCode.PING_REQUEST, src, in);
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        // WRITE NOTHING
    }
    
    public String toString() {
        return "PingRequest: " + getContact();
    }
}
