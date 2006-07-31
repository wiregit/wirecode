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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.event.StoreEvent;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.util.EntryImpl;

/**
 * 
 */
public class StoreResponseHandler extends AbstractResponseHandler<StoreEvent> {

    private static final Log LOG = LogFactory.getLog(StoreResponseHandler.class);
    
    private Contact node;
    private QueryKey queryKey;
    
    private DHTValue value;
    
    private List<Contact> targets = new ArrayList<Contact>();
    
    private int countDown = 0;
    
    public StoreResponseHandler(Context context, DHTValue value) {
        super(context);
        this.value = value;
    }

    public StoreResponseHandler(Context context, Contact node, 
            QueryKey queryKey, DHTValue value) {
        super(context);
        
        this.node = node;
        this.queryKey = queryKey;
        this.value = value;
    }
    
    @Override
    protected void start() throws Exception {
        
        List<Entry<Contact,QueryKey>> nodes = null;
        
        if (node == null && queryKey == null) {
            FindNodeResponseHandler handler 
                = new FindNodeResponseHandler(context, value.getValueID().toNodeID());
            nodes = handler.call().getNodes();   
        } else {
            Entry<Contact, QueryKey> entry 
                = new EntryImpl<Contact, QueryKey>(node, queryKey);
            nodes = Arrays.asList(entry);
        }
        
        //System.out.println("Storing at:");
        //System.out.println(CollectionUtils.toString(nodes));
        
        storeAt(nodes);
    }
    
    private synchronized void storeAt(List<? extends Map.Entry<Contact,QueryKey>> nodes) throws Exception {
        for (Entry<Contact, QueryKey> entry : nodes) {
            Contact node = entry.getKey();
            QueryKey queryKey = entry.getValue();
            
            if (context.isLocalNodeID(node.getNodeID())) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Skipping local Node as KeyValue is already stored at this Node");
                }
                
                synchronized (targets) {
                    targets.add(node);                    
                }
                continue;
            }
            
            if (queryKey == null) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Cannot store " + value + " at " 
                            + node + " because we have no QueryKey for it");
                }
                continue;
            }
            
            StoreRequest request = context.getMessageHelper()
                .createStoreRequest(node.getContactAddress(), queryKey, value);
            
            context.getMessageDispatcher().send(node, request, this);
            countDown++;
        }
        
        fireEventIfFinished();
    }

    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        synchronized (targets) {
            targets.add(message.getContact());
        }
        fireEventIfFinished();
    }
    
    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        fireEventIfFinished();
    }
    
    @Override
    protected void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        e.printStackTrace();
        fireEventIfFinished();
    }

    private synchronized void fireEventIfFinished() {
        assert (countDown >= 0);
        if (countDown == 0) {
            value.publishedTo(targets.size());
            setReturnValue(new StoreEvent(value, targets));
        }
        countDown--;
    }
}
