/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

package org.limewire.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.limewire.collection.Trie.Cursor;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.LookupRequest;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.result.FindNodeResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.statistics.FindNodeLookupStatisticContainer;
import org.limewire.security.QueryKey;


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
        int routeTableFailures = getRouteTableFailures();
        
        lookupStat.setHops(currentHop, false);
        lookupStat.setTime((int)time, false);
        
        Map<Contact, QueryKey> nearest = getNearestContacts();
        Collection<Contact> collisions = getCollisions();
        
        FindNodeResult result = new FindNodeResult(getLookupID(), nearest, 
                collisions, time, currentHop, routeTableFailures);
        
        // We can use the result from a Node lookup to estimate the DHT size
        context.updateEstimatedSize(nearest.keySet());
        
        setReturnValue(result);
    }
    
    /**
     * Returns the k-closest Contacts sorted by their closeness
     * to the given lookup key
     */
    private Map<Contact, QueryKey> getNearestContacts() {
        return getContacts(getResultSetSize());
    }
    
    /**
     * Returns count number of Contacts sorted by their closeness
     * to the given lookup key
     */
    private Map<Contact, QueryKey> getContacts(int count) {
        if (count < 0) {
            count = responses.size();
        }
        
        final int maxCount = count;
        
        // Use a LinkedHashMap which preserves the insertion order...
        final Map<Contact, QueryKey> nearest = new LinkedHashMap<Contact, QueryKey>();
        
        responses.select(lookupId, new Cursor<KUID, Entry<Contact,QueryKey>>() {
            public SelectStatus select(Entry<? extends KUID, ? extends Entry<Contact, QueryKey>> entry) {
                Entry<Contact, QueryKey> e = entry.getValue();
                nearest.put(e.getKey(), e.getValue());
                
                if (nearest.size() < maxCount) {
                    return SelectStatus.CONTINUE;
                }
                
                return SelectStatus.EXIT;
            }
        });
        
        return nearest;
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
