/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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
 
package org.limewire.mojito.messages.impl;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map.Entry;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.messages.StoreResponse;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;


/**
 * An implementation of StoreResponse
 */
public class StoreResponseImpl extends AbstractResponseMessage
        implements StoreResponse {

    private final Collection<? extends Entry<KUID, Status>> status;

    public StoreResponseImpl(Context context, 
            Contact contact, MessageID messageId, 
            Collection<? extends Entry<KUID, Status>> status) {
        super(context, OpCode.STORE_RESPONSE, contact, messageId);

        this.status = status;
    }

    public StoreResponseImpl(Context context, SocketAddress src, 
            MessageID messageId, Version version, MessageInputStream in) throws IOException {
        super(context, OpCode.STORE_RESPONSE, src, messageId, version, in);
        
        this.status = in.readStoreStatus();
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeStoreStatus(status);
    }

    public Collection<? extends Entry<KUID, Status>> getStatus() {
        return status;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder("StoreResponse:\n");
        int i = 0;
        for (Entry<KUID, Status> e : status) {
            buffer.append(i++).append(": valueId=").append(e.getKey())
                .append(", status=").append(e.getValue()).append("\n");
        }
        return buffer.toString();
    }
}
