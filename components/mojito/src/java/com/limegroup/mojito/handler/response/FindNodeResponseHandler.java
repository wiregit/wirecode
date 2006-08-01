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

package com.limegroup.mojito.handler.response;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.FindNodeEvent;
import com.limegroup.mojito.messages.RequestMessage;

/**
 * 
 */
public class FindNodeResponseHandler 
        extends LookupResponseHandler<FindNodeEvent> {

    private List<Entry<Contact, QueryKey>> nodes = Collections.emptyList();
    
    public FindNodeResponseHandler(Context context, KUID lookupId) {
        super(context, lookupId.assertNodeID());
    }
    
    public FindNodeResponseHandler(Context context, Contact force, KUID lookupId) {
        super(context, force, lookupId.assertNodeID());
    }
    
    public FindNodeResponseHandler(Context context, KUID lookupId, int resultSetSize) {
        super(context, lookupId.assertNodeID(), resultSetSize);
    }

    @Override
    protected boolean isValueLookup() {
        return false;
    }

    @Override
    protected RequestMessage createRequest(SocketAddress address) {
        return context.getMessageHelper().createFindNodeRequest(address, lookupId);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleFoundNodes(List<? extends Entry<Contact, QueryKey>> nodes) {
        this.nodes = (List<Entry<Contact, QueryKey>>)nodes;
    }

    @Override
    protected void handleLookupFinished(long time, int hops) {
        setReturnValue(new FindNodeEvent(getLookupID(), nodes));
    }
}
