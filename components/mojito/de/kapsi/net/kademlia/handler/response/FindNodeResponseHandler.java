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

import com.limegroup.gnutella.dht.statistics.FindNodeLookupStatisticContainer;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.FindNodeListener;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.settings.KademliaSettings;

public class FindNodeResponseHandler extends LookupResponseHandler {
    
    private FindNodeListener l;
    
    public FindNodeResponseHandler(Context context, KUID lookup, FindNodeListener l) {
        super(context, lookup, KademliaSettings.NODE_LOOKUP_TIMEOUT.getValue());
        lookupStat = new FindNodeLookupStatisticContainer(context,lookup);
        this.l = l;
    }
    
    protected Message createMessage(KUID lookup) {
        return context.getMessageFactory().createFindNodeRequest(lookup);
    }
    
    protected boolean isValueLookup() {
        return false;
    }
    
    protected void finishValueLookup(KUID lookup, Collection keyValues, long time) {
        throw new RuntimeException("This handler is responsible for FIND_NODE responses");
    }

    protected void finishNodeLookup(final KUID lookup, final Collection nodes, 
            final Map queryKeys, final long time) {
        
        if (l != null) {
            context.fireEvent(new Runnable() {
                public void run() {
                    l.foundNodes(lookup, nodes, queryKeys, time);
                }
            });
        }
    }
}
