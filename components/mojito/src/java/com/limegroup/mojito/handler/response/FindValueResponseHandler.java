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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.event.FindValueEvent;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.FindValueLookupStatisticContainer;
import com.limegroup.mojito.util.EntryImpl;

/**
 * 
 */
public class FindValueResponseHandler extends LookupResponseHandler<FindValueEvent> {
	
	private static final Log LOG = LogFactory.getLog(FindValueResponseHandler.class);

    private List<Entry<Contact,Collection<KeyValue>>> values = new ArrayList<Entry<Contact,Collection<KeyValue>>>();
    
    /** The number of value locations we've found if this is a value lookup */
    private int foundValueLocs = 0;
    
    public FindValueResponseHandler(Context context, KUID lookupId) {
        super(context, lookupId.assertValueID());
        lookupTimeout = KademliaSettings.VALUE_LOOKUP_TIMEOUT.getValue();
        lookupStat = new FindValueLookupStatisticContainer(context, lookupId);
        init();
    }

    @Override
	protected synchronized void response(ResponseMessage message, long time) throws IOException {
		super.response(message, time);
		
		
		if(message instanceof FindValueResponse) {
			FindValueResponse response = (FindValueResponse) message;
			handleFindValueResponse(response, time);
		} else {
			handleFindNodeResponse((FindNodeResponse)message, time);
		}
		
		postHandle();
	}
    
	private void handleFindValueResponse(FindValueResponse response, long time) throws IOException {
        
        long totalTime = time();
        Collection<KeyValue> values = response.getValues();
        
        if (values.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(response.getContact()
                    + " returned an empty KeyValueCollection for " + lookupId);
            }
            
            lookupStep();
            
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace(response.getContact()
                        + " returned KeyValues for "
                        + lookupId + " after "
                        + queried.size() + " queried Nodes and a total time of "
                        + totalTime + "ms");
            }
        
            if (foundValueLocs == 0) {
                lookupStat.setHops(currentHop, true);
                lookupStat.setTime((int)totalTime, true);
            }
            foundValueLocs++;
            
            handleFoundValues(response.getContact(), values);
            
            if (isExhaustiveValueLookup()) {
                lookupStep();
            } else {
                setLookupFinished(true);
            }
        }
    }
	
    @Override
	protected void doFinishLookup(long time) {
		setLookupFinished(true);
		if (foundValueLocs == 0) {
            ((FindValueLookupStatisticContainer)lookupStat).FIND_VALUE_FAILURE.incrementStat();
        } else {
            ((FindValueLookupStatisticContainer)lookupStat).FIND_VALUE_OK.incrementStat();
        }
		
		setReturnValue(new FindValueEvent(getLookupID(), values));
	}



	private boolean isExhaustiveValueLookup() {
    	return KademliaSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue();
    }

	@Override
    protected RequestMessage createRequest(SocketAddress address) {
        return context.getMessageHelper().createFindValueRequest(address, lookupId);
    }

    protected void handleFoundValues(Contact node, Collection<KeyValue> c) {
        Entry<Contact, Collection<KeyValue>> entry 
            = new EntryImpl<Contact, Collection<KeyValue>>(node, c);
        values.add(entry);
    }

}
