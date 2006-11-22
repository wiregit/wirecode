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
import java.util.Collection;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.result.FindValueResult;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.FindValueLookupStatisticContainer;

/**
 * The FindNodeResponseHandler class implements FIND_VALUE specific features.
 */
public class FindValueResponseHandler extends LookupResponseHandler<FindValueResult> {

    private FindValueLookupStatisticContainer lookupStat;
    
    public FindValueResponseHandler(Context context, KUID lookupId) {
        super(context, LookupType.FIND_VALUE, lookupId);
        setExhaustive(KademliaSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue());
        lookupStat = new FindValueLookupStatisticContainer(context, lookupId);
    }
    
    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        super.response(message, time);
        lookupStat.addReply();
    }

    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        super.timeout(nodeId, dst, message, time);
        lookupStat.addTimeout();
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
        
        Collection<FindValueResponse> responses = getValues();
        
        if (responses.isEmpty()) {
            lookupStat.FIND_VALUE_FAILURE.incrementStat();
        } else {
            lookupStat.FIND_VALUE_OK.incrementStat();
        }
        
        setReturnValue(new FindValueResult(context, getLookupID(), responses, time, hop));
    }
}
