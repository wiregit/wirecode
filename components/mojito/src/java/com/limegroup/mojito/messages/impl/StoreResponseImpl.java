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

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.StoreResponse;


public class StoreResponseImpl extends AbstractResponseMessage
        implements StoreResponse {

    private KUID valueId;
    private int status;

    public StoreResponseImpl(int vendor, int version, ContactNode node,
            KUID messageId, KUID valueId, int status) {
        super(STORE_RESPONSE, vendor, version, node, messageId);

        this.valueId = valueId;
        this.status = status;
    }

    public StoreResponseImpl(SocketAddress src, ByteBuffer data) throws IOException {
        super(STORE_RESPONSE, src, data);
        
        this.valueId = KUID.createValueID(data);
        this.status = data.get() & 0xFF;
    }
    
    public KUID getValueID() {
        return valueId;
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeKUID(valueId);
        out.writeByte(status);
    }

    public int getStatus() {
        return status;
    }
    
    public String toString() {
        return "StoreResponse: valueId=" + valueId + ", status=" + status;
    }
}
