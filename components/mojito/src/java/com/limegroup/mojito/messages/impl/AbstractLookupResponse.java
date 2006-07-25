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
import com.limegroup.mojito.messages.LookupResponse;
import com.limegroup.mojito.messages.MessageID;

/**
 * An abstract class for LookupResponse implementations
 */
abstract class AbstractLookupResponse extends AbstractResponseMessage 
	implements LookupResponse {

    public AbstractLookupResponse(Context context, 
            OpCode opcode, Contact contact, MessageID messageId) {
        super(context, opcode, contact, messageId);
    }
    
    public AbstractLookupResponse(Context context, 
            OpCode opcode, SocketAddress src, 
            MessageID messageId, int version, MessageInputStream in) throws IOException {
        super(context, opcode, messageId, version, src, in);
    }
}
