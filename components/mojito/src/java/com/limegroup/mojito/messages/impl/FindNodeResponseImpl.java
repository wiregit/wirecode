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

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.util.ByteBufferUtils;


public class FindNodeResponseImpl extends AbstractLookupResponse
        implements FindNodeResponse {

    private QueryKey queryKey;

    private Collection nodes;

    public FindNodeResponseImpl(int vendor, int version, ContactNode node,
            KUID messageId, QueryKey queryKey, Collection nodes) {
        super(FIND_NODE_RESPONSE, vendor, version, node, messageId);

        this.queryKey = queryKey;
        this.nodes = nodes;
    }
    
    public FindNodeResponseImpl(SocketAddress src, ByteBuffer data) throws IOException {
        super(FIND_NODE_RESPONSE, src, data);
        
        this.queryKey = ByteBufferUtils.getQueryKey(data);
        this.nodes = ByteBufferUtils.getContactNodes(data);
    }
    
    public QueryKey getQueryKey() {
        return queryKey;
    }

    public Collection getNodes() {
        return nodes;
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeQueryKey(queryKey);
        out.writeContactNodes(nodes);
    }
    
    public String toString() {
        return "FindNodeResponse: queryKey=" + queryKey + ", nodes=" + nodes;
    }
}
