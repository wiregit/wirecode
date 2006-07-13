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
import com.limegroup.mojito.messages.ResponseMessage;

/**
 * An abstract base class for response messages
 */
abstract class AbstractResponseMessage extends AbstractDHTMessage
        implements ResponseMessage {

    public AbstractResponseMessage(Context context, 
            OpCode opcode, Contact contact, KUID messageId) {
        super(context, opcode, contact, messageId);
    }
    
    public AbstractResponseMessage(Context context, 
            OpCode opcode, SocketAddress src, ByteBuffer... data) 
            throws IOException {
        super(context, opcode, src, data);
    }
}
