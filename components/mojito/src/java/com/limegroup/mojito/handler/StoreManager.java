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
 
package com.limegroup.mojito.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.event.LookupListener;
import com.limegroup.mojito.event.StoreListener;
import com.limegroup.mojito.handler.response.StoreResponseHandler;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;

/**
 * The StoreManager performs a lookup for the Value ID we're
 * going to store and stores the KeyValue at the k closest 
 * Nodes of the result set.
 */
public class StoreManager implements LookupListener {
    
    private static final Log LOG = LogFactory.getLog(StoreManager.class);
    
    private Context context;
    
    private KeyValue keyValue;
    private StoreListener listener;
    
    public StoreManager(Context context) {
        this.context = context;
    }
    
    public void store(KeyValue keyValue, 
            StoreListener listener) throws IOException {
        this.keyValue = keyValue;
        this.listener = listener;
        
        KUID nodeId = keyValue.getKey().toNodeID();
        this.context.lookup(nodeId, this);
    }
    
    public void response(ResponseMessage response, long time) {
    }

    public void timeout(KUID nodeId, SocketAddress address, 
            RequestMessage request, long time) {
    }
    
    public void found(KUID lookup, Collection c, long time) {
        
    }
    
    public void finish(KUID lookup, Collection c, long time) {
        // List of ContactNodes where we stored the KeyValues.
        final List<Contact> storeTargets = new ArrayList<Contact>(c.size());
        
        for(Iterator it = c.iterator(); it.hasNext(); ) {
            Entry<Contact, QueryKey> entry 
                    = (Entry<Contact, QueryKey>)it.next();
            
            Contact node = entry.getKey();
            QueryKey queryKey = entry.getValue();
            
            if (context.isLocalNodeID(node.getNodeID())) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Skipping local Node as KeyValue is already stored at this Node");
                }
                storeTargets.add(node);
                continue;
            }
            
            if (queryKey == null) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Cannot store " + keyValue + " at " 
                            + node + " because we have no QueryKey for it");
                }
                continue;
            }
            
            try {
                new StoreResponseHandler(context, queryKey, keyValue).store(node);
                storeTargets.add(node);
            } catch (IOException err) {
                LOG.error("Failed to store KeyValue", err);
            }
        }
        
        keyValue.setNumLocs(storeTargets.size());
        
        if (listener != null) {
            context.fireEvent(new Runnable() {
                public void run() {
                    listener.store(keyValue, storeTargets);
                }
            });
        }
    }
}
