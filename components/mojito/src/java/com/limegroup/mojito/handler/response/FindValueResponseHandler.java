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
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.event.FindValueEvent;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.LookupRequest;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.FindValueLookupStatisticContainer;

/**
 * The FindNodeResponseHandler class implements FIND_VALUE specific features.
 */
public class FindValueResponseHandler extends LookupResponseHandler<FindValueEvent> {

    private static final Log LOG = LogFactory.getLog(FindValueResponseHandler.class);
    
    private List<FindValueResponse> responses = new ArrayList<FindValueResponse>();
    
    private FindValueLookupStatisticContainer lookupStat;
    
    public FindValueResponseHandler(Context context, KUID lookupId) {
        super(context, lookupId);
        init();
    }

    private void init() {
        lookupStat = new FindValueLookupStatisticContainer(context, lookupId);
    }
    
    @Override
    protected int getParallelLookups() {
        return KademliaSettings.FIND_VALUE_PARALLEL_LOOKUPS.getValue();
    }

    @Override
    protected boolean isLookupTimeout(long time) {
        long lookupTimeout = KademliaSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue();
        return lookupTimeout > 0L && time >= lookupTimeout;
    }

    /**
     * Returns true of this is an exhaustive FIND_VALUE lookup
     */
    private boolean isExhaustiveValueLookup() {
        return KademliaSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue();
    }
    
    @Override
    protected synchronized void response(ResponseMessage message, long time) throws IOException {
        if (message instanceof FindNodeResponse) {
            super.response(message, time);
            return;
        }
        
        FindValueResponse response = (FindValueResponse)message;
        
        Contact node = response.getContact();
        Collection<KUID> keys = response.getKeys();
        Collection<DHTValue> values = response.getValues();
        
        if (keys.isEmpty() && values.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(node + " returned neither keys nor values for " + lookupId);
            }
            
            super.response(message, time);
            return;
        }
        
        responses.add(response);
        
        if (isExhaustiveValueLookup()) {
            super.response(message, time);
        } else {
            finishLookup();
        }
        
        lookupStat.addReply();
    }

    @Override
    protected synchronized void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        super.timeout(nodeId, dst, message, time);
        lookupStat.addTimeout();
    }

    @Override
    protected LookupRequest createLookupRequest(SocketAddress address) {
        Collection<KUID> noKeys = Collections.emptySet();
        return context.getMessageHelper().createFindValueRequest(address, lookupId, noKeys);
    }
    
    @Override
    protected boolean sendLookupRequest(Contact node) throws IOException {
        if (super.sendLookupRequest(node)) {
            lookupStat.addRequest();
            return true;
        }
        return false;
    }

    @Override
    protected void finishLookup() {
        long time = getElapsedTime();
        int hop = getCurrentHop();
        
        if (responses.isEmpty()) {
            lookupStat.FIND_VALUE_FAILURE.incrementStat();
        } else {
            lookupStat.FIND_VALUE_OK.incrementStat();
        }
        
        setReturnValue(new FindValueEvent(context, getLookupID(), responses, time, hop));
    }
}
