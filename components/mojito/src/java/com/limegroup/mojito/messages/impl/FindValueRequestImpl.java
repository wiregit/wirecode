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
import java.util.Collection;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageInputStream;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.MessageID;

/**
 * An implementation of FindValueRequest
 */
public class FindValueRequestImpl extends AbstractLookupRequest
        implements FindValueRequest {
    
    private Collection<KUID> keys;
    
    public FindValueRequestImpl(Context context, 
            Contact contact, MessageID messageId, 
            KUID lookupId, Collection<KUID> keys) {
        super(context, OpCode.FIND_VALUE_REQUEST, 
                contact, messageId, lookupId);
        
        this.keys = keys;
    }
    
    public FindValueRequestImpl(Context context, SocketAddress src, 
            MessageID messageId, int version, MessageInputStream in) throws IOException {
        super(context, OpCode.FIND_VALUE_REQUEST, src, messageId, version, in);
        
        this.keys = in.readKUIDs();
    }
    
    public Collection<KUID> getKeys() {
        return keys;
    }
    
    @Override
    protected void writeBody(MessageOutputStream out) throws IOException {
        super.writeBody(out);
        out.writeKUIDs(keys);
    }

    public String toString() {
        return "FindValueRequest: " + lookupId;
    }
}
