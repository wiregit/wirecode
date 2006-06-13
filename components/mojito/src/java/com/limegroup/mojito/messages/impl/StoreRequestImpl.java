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

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.util.ByteBufferUtils;


public class StoreRequestImpl extends AbstractRequestMessage
        implements StoreRequest {

    private QueryKey queryKey;
    private KeyValue keyValue;

    public StoreRequestImpl(int vendor, int version,
            ContactNode node, KUID messageId,
            QueryKey queryKey, KeyValue keyValue) {
        super(STORE_REQUEST, vendor, version, node, messageId);

        this.queryKey = queryKey;
        this.keyValue = keyValue;
    }
    
    public StoreRequestImpl(SocketAddress src, ByteBuffer data) throws IOException {
        super(STORE_REQUEST, src, data);
        
        this.queryKey = ByteBufferUtils.getQueryKey(data);
        this.keyValue = ByteBufferUtils.getKeyValue(data);
    }
    
    public QueryKey getQueryKey() {
        return queryKey;
    }

    public KeyValue getKeyValue() {
        return keyValue;
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeQueryKey(queryKey);
        out.writeKeyValue(keyValue);
    }

    public String toString() {
        return "StoreRequest: " + keyValue.toString();
    }
}
