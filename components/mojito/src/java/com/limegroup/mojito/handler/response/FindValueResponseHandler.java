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
import java.util.List;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.util.KeyValueCollection;

/**
 * 
 */
public class FindValueResponseHandler extends LookupResponseHandler<List<KeyValueCollection>> {

    private List<KeyValueCollection> values = new ArrayList<KeyValueCollection>();
    
    public FindValueResponseHandler(Context context, KUID lookupId) {
        super(context, lookupId.assertValueID());
    }

    @Override
    protected RequestMessage createRequest(SocketAddress address) {
        return context.getMessageHelper().createFindValueRequest(address, lookupId);
    }

    @Override
    protected void handleFoundValues(KeyValueCollection c) {
        values.add(c);
    }

    @Override
    protected void handleLookupFinished(long time, int hops) {
        setReturnValue(values);
    }
}
