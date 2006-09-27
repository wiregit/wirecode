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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.Trie.Cursor;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.FindNodeEvent;
import com.limegroup.mojito.messages.LookupRequest;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.FindNodeLookupStatisticContainer;

/**
 * The FindNodeResponseHandler class implements FIND_NODE specific features.
 */
public class FindNodeResponseHandler 
        extends LookupResponseHandler<FindNodeEvent> {

    //private static final Log LOG = LogFactory.getLog(FindNodeResponseHandler.class);
    
    private FindNodeLookupStatisticContainer lookupStat;
    
    public FindNodeResponseHandler(Context context, KUID lookupId) {
        super(context, lookupId);
        init();
    }
    
    public FindNodeResponseHandler(Context context, Contact force, KUID lookupId) {
        super(context, force, lookupId);
        init();
    }
    
    public FindNodeResponseHandler(Context context, KUID lookupId, int resultSetSize) {
        super(context, lookupId, resultSetSize);
        init();
    }
    
    public FindNodeResponseHandler(Context context, Contact forcedContact, 
            KUID lookupId, int resultSetSize) {
        super(context, forcedContact, lookupId, resultSetSize);
        init();
    }
    
    private void init() {
        lookupStat = new FindNodeLookupStatisticContainer(context, lookupId);
    }
    
    @Override
    protected boolean isGlobalTimeout(long time) {
        long lookupTimeout = KademliaSettings.FIND_NODE_LOOKUP_TIMEOUT.getValue();
        return lookupTimeout > 0L && time >= lookupTimeout;
    }

    @Override
    protected int getParallelLookups() {
        return KademliaSettings.FIND_NODE_PARALLEL_LOOKUPS.getValue();
    }
    
    @Override
    protected LookupRequest createLookupRequest(SocketAddress address) {
        return context.getMessageHelper().createFindNodeRequest(address, lookupId);
    }

    @Override
    protected synchronized void response(ResponseMessage message, long time) throws IOException {
        super.response(message, time);
        lookupStat.addReply();
    }

    @Override
    protected synchronized void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
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
        
        // Use a LinkedHashMap which preserves the insertion order...
        final Map<Contact, QueryKey> nearest = new LinkedHashMap<Contact, QueryKey>();
        responses.select(lookupId, new Cursor<KUID, Entry<Contact,QueryKey>>() {
            public SelectStatus select(Entry<? extends KUID, ? extends Entry<Contact, QueryKey>> entry) {
                Entry<Contact, QueryKey> e = entry.getValue();
                nearest.put(e.getKey(), e.getValue());
                
                if (nearest.size() >= getResultSetSize()) {
                    return SelectStatus.EXIT;
                }
                
                return SelectStatus.CONTINUE;
            }
        });
        
        FindNodeEvent evt = new FindNodeEvent(getLookupID(), nearest, 
                collisions, time, hop, routeTableFailures);
        
        // TODO We can use the result from a Node lookup to estimate the DHT size
        //context.updateEstimatedSize(nearest.keySet());
        
        setReturnValue(evt);
    }
}
