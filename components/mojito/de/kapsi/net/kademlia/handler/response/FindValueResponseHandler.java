/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package de.kapsi.net.kademlia.handler.response;

import java.util.Collection;
import java.util.Map;

import com.limegroup.gnutella.dht.statistics.FindValueLookupStatisticContainer;


import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.FindValueListener;
import de.kapsi.net.kademlia.messages.Message;

public class FindValueResponseHandler extends LookupResponseHandler {
    
    private FindValueListener l;

    public FindValueResponseHandler(Context context, 
            KUID lookup, FindValueListener l) {
        super(context, lookup);
        lookupStat = new FindValueLookupStatisticContainer(context,lookup);
        this.l = l;
    }
    
    protected boolean isValueLookup() {
        return true;
    }
    
    protected Message createMessage(KUID lookup) {
        return context.getMessageFactory().createFindValueRequest(lookup);
    }

    protected void finishValueLookup(final KUID lookup, final Collection keyValues, final long time) {
        if (l != null) {
            context.getEventDispatcher().add(new Runnable() {
                public void run() {
                    l.foundValue(lookup, keyValues, time);
                }
            });
        }
    }

    protected void finishNodeLookup(KUID lookup, Collection nodes, Map queryKeys, long time) {
        throw new RuntimeException("This handler is responsible for FIND_VALUE responses");
    }
}
