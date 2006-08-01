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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.FindNodeEvent;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.FindNodeLookupStatisticContainer;
import com.limegroup.mojito.util.TrieUtils;

/**
 * 
 */
public class FindNodeResponseHandler 
        extends LookupResponseHandler<FindNodeEvent> {
	
    private List<Entry<Contact, QueryKey>> nodes = Collections.emptyList();
    
    public FindNodeResponseHandler(Context context, KUID lookupId) {
        super(context, lookupId.assertNodeID());
        initializeHandler();
    }
    
    public FindNodeResponseHandler(Context context, Contact force, KUID lookupId) {
        super(context, force, lookupId.assertNodeID());
        initializeHandler();
    }
    
    public FindNodeResponseHandler(Context context, KUID lookupId, int resultSetSize) {
        super(context, lookupId.assertNodeID(), resultSetSize);
        initializeHandler();
    }
    
    private void initializeHandler() {
    	lookupTimeout = KademliaSettings.NODE_LOOKUP_TIMEOUT.getValue();
        lookupStat = new FindNodeLookupStatisticContainer(context, lookupId);
        init();
    }
    
    @Override
	protected synchronized void response(ResponseMessage message, long time) throws IOException {
		super.response(message, time);
		
		if(message instanceof FindValueResponse) {
			//Some Idot sent us a FIND_VALUE response for a
            // FIND_NODE lookup! Ignore? We're losing one
            // parallel lookup (temporarily) if we do nothing.
            // I think it's better to kick off a new lookup
            // now rather than to wait for a yet another
            // response/lookup that would re-activate this one.
            lookupStep();
		} else {
			handleFindNodeResponse((FindNodeResponse)message, time);
		}
		postHandle();
	}
    
	@Override
	protected void doFinishLookup(long time) {
		setLookupFinished(true);
		lookupStat.setHops(currentHop, false);
        lookupStat.setTime((int)time, false);
        
        // addResponse(ContactNode) limits the size of the
        // Trie to K and we can thus use the size method of it!
        nodes = TrieUtils.select(responses, lookupId, responses.size());
        
        setReturnValue(new FindNodeEvent(getLookupID(),nodes));
	}

	@Override
    protected RequestMessage createRequest(SocketAddress address) {
        return context.getMessageHelper().createFindNodeRequest(address, lookupId);
    }

}
