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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.io.MessageInputStream;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.FindValueResponse;

/**
 * An implementation of FindValueResponse
 */
public class FindValueResponseImpl extends AbstractLookupResponse
        implements FindValueResponse {

    private Collection<KeyValue> values;

    public FindValueResponseImpl(Context context, 
            Contact contact, KUID messageId, 
            Collection<KeyValue> values) {
        super(context, OpCode.FIND_VALUE_RESPONSE, contact, messageId);

        this.values = values;
    }

    public FindValueResponseImpl(Context context, 
            SocketAddress src, ByteBuffer... data) throws IOException {
        super(context, OpCode.FIND_VALUE_RESPONSE, src, data);
        
        MessageInputStream in = getMessageInputStream();
        
        this.values = in.readKeyValues();
    }
    
    public Collection<KeyValue> getValues() {
        return values;
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeKeyValues(values);
    }
    
    public String toString() {
        return "FindValueResponse: " + values;
    }
}
