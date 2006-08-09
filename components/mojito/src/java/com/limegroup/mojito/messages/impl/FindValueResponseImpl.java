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
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.io.MessageInputStream;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.MessageID;

/**
 * An implementation of FindValueResponse
 */
public class FindValueResponseImpl extends AbstractLookupResponse
        implements FindValueResponse {

    private Collection<KUID> keys;
    
    private Collection<DHTValue> values;

    @SuppressWarnings("unchecked")
    public FindValueResponseImpl(Context context, 
            Contact contact, MessageID messageId, 
            Collection<KUID> keys,
            Collection<? extends DHTValue> values) {
        super(context, OpCode.FIND_VALUE_RESPONSE, contact, messageId);
        
        this.keys = keys;
        this.values = (Collection<DHTValue>)values;
    }

    public FindValueResponseImpl(Context context, SocketAddress src, 
            MessageID messageId, int version, MessageInputStream in) throws IOException {
        super(context, OpCode.FIND_VALUE_RESPONSE, src, messageId, version, in);
        
        this.keys = in.readNodeIDs();
        this.values = in.readDHTValues(getContact());
    }
    
    public Collection<KUID> getKeys() {
        return keys;
    }
    
    public Collection<DHTValue> getValues() {
        return values;
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeKUIDs(keys);
        out.writeDHTValues(values);
    }
    
    public String toString() {
        return "FindValueResponse: " + values;
    }
}
