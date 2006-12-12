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
import java.util.Map;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.LookupRequest;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.result.FindNodeResult;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.FindNodeLookupStatisticContainer;

/**
 * The FindNodeResponseHandler class implements FIND_NODE specific features.
 */
public class FindNodeResponseHandler 
        extends LookupResponseHandler<FindNodeResult> {
    
    private FindNodeLookupStatisticContainer lookupStat;
    
    public FindNodeResponseHandler(Context context, KUID lookupId) {
        super(context, lookupId);
        init();
    }
    
    public FindNodeResponseHandler(Context context, Contact forcedContact, KUID lookupId) {
        super(context, lookupId);
        addForcedContact(forcedContact);
        init();
    }
    
    public FindNodeResponseHandler(Context context, KUID lookupId, int resultSetSize) {
        super(context, lookupId);
        setResultSetSize(resultSetSize);
        init();
    }
    
    public FindNodeResponseHandler(Context context, Contact forcedContact, 
            KUID lookupId, int resultSetSize) {
        super(context, lookupId);
        addForcedContact(forcedContact);
        setResultSetSize(resultSetSize);
        init();
    }
    
    private void init() {
        lookupStat = new FindNodeLookupStatisticContainer(context, getLookupID());
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
        int routeTableFailures = getRouteTableFailures();
        
        lookupStat.setHops(hop, false);
        lookupStat.setTime((int)time, false);
        
        Map<Contact, QueryKey> nearest = getNearestContacts();
        Collection<Contact> collisions = getCollisions();
        
        FindNodeResult result = new FindNodeResult(getLookupID(), nearest, 
                collisions, time, hop, routeTableFailures);
        
        // We can use the result from a Node lookup to estimate the DHT size
        context.updateEstimatedSize(nearest.keySet());
        
        setReturnValue(result);
    }
    
    /**
     * Returns a Collection of Contacts that did collide with the
     * local Node ID
     */
    public Collection<Contact> getCollisions() {
        return collisions;
    }
    
    protected int getDefaultParallelism() {
        return KademliaSettings.FIND_NODE_PARALLEL_LOOKUPS.getValue();
    }
    
    protected boolean isTimeout(long time) {
        long lookupTimeout = KademliaSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue();
        return lookupTimeout > 0L && time >= lookupTimeout;
    }
    
    protected LookupRequest createLookupRequest(SocketAddress addr) {
        return context.getMessageHelper().createFindNodeRequest(addr, lookupId);
    }

    @Override
    protected boolean nextStep(ResponseMessage message) throws IOException {
        if (!(message instanceof FindNodeResponse))
            throw new IllegalArgumentException("this is find node handler");
        return handleNodeResponse((FindNodeResponse)message);
    }
    
    
}
