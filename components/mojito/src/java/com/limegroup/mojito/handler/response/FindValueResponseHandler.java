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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.event.FindValueEvent;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.util.EntryImpl;

/**
 * 
 */
public class FindValueResponseHandler extends LookupResponseHandler<FindValueEvent> {

    private List<Entry<Contact,Collection<KeyValue>>> values = new ArrayList<Entry<Contact,Collection<KeyValue>>>();
    
    public FindValueResponseHandler(Context context, KUID lookupId) {
        super(context, lookupId.assertValueID());
    }

    @Override
    protected RequestMessage createRequest(SocketAddress address) {
        return context.getMessageHelper().createFindValueRequest(address, lookupId);
    }

    @Override
    protected void handleFoundValues(Contact node, Collection<KeyValue> c) {
        Entry<Contact, Collection<KeyValue>> entry 
            = new EntryImpl<Contact, Collection<KeyValue>>(node, c);
        values.add(entry);
    }

    @Override
    protected void handleLookupFinished(long time, int hops) {
        setReturnValue(new FindValueEvent(getLookupID(), values));
    }
}
