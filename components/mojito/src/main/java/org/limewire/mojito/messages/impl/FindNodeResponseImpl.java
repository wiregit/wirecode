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

import org.limewire.mojito.Context;
import org.limewire.mojito.io.MessageInputStream;
import org.limewire.mojito.io.MessageOutputStream;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;
import org.limewire.security.QueryKey;


/**
 * An implementation of FindNodeResponse
 */
public class FindNodeResponseImpl extends AbstractLookupResponse
        implements FindNodeResponse {

    private QueryKey queryKey;

    private Collection<? extends Contact> nodes;

    public FindNodeResponseImpl(Context context, 
            Contact contact, MessageID messageId, 
            QueryKey queryKey, Collection<? extends Contact> nodes) {
        super(context, OpCode.FIND_NODE_RESPONSE, contact, messageId);

        this.queryKey = queryKey;
        this.nodes = nodes;
    }
    
    public FindNodeResponseImpl(Context context, SocketAddress src, 
            MessageID messageId, Version version, MessageInputStream in) throws IOException {
        super(context, OpCode.FIND_NODE_RESPONSE, src, messageId, version, in);
        
        this.queryKey = in.readQueryKey();
        this.nodes = in.readContacts();
    }
    
    public QueryKey getQueryKey() {
        return queryKey;
    }

    public Collection<? extends Contact> getNodes() {
        return nodes;
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeQueryKey(queryKey);
        out.writeContacts(nodes);
    }
    
    public String toString() {
        return "FindNodeResponse: queryKey=" + queryKey + ", nodes=" + nodes;
    }
}
