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
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageInputStream;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.MessageID;

/**
 * An implementation of FindNodeRequest
 */
public class FindNodeRequestImpl extends AbstractLookupRequest
        implements FindNodeRequest {

    public FindNodeRequestImpl(Context context, 
            Contact contact, MessageID messageId, KUID lookupId) {
        super(context, OpCode.FIND_NODE_REQUEST, 
                contact, messageId, lookupId);
    }
    
    public FindNodeRequestImpl(Context context, SocketAddress src, 
            MessageID messageId, int version, MessageInputStream in) throws IOException {
        super(context, OpCode.FIND_NODE_REQUEST, src, messageId, version, in);
    }
    
    public String toString() {
        return "FindNodeRequest: " + lookupId;
    }
}
