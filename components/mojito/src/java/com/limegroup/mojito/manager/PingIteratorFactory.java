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

package com.limegroup.mojito.manager;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.response.PingResponseHandler;
import com.limegroup.mojito.handler.response.PingResponseHandler.PingIterator;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.settings.ContextSettings;
import com.limegroup.mojito.util.ContactUtils;
import com.limegroup.mojito.util.EntryImpl;

/**
 * A Factory class that provides various PingIterator
 * implementations for the PingManager
 */
class PingIteratorFactory {
    
    private PingIteratorFactory() {
    }
    
    /**
     * A ContactPinger sends ping requests to a Set of Contacts
     */
    static class ContactPinger implements PingIterator {
        
        final Iterator<Contact> nodes;
        
        public ContactPinger(Contact node) {
            this(Collections.singleton(node));
        }
        
        @SuppressWarnings("unchecked")
        public ContactPinger(Set<? extends Contact> nodes) {
            if (nodes == null) {
                throw new NullPointerException("Set<Contact> is null");
            }
            
            for (Contact c : nodes) {
                if (c == null) {
                    throw new NullPointerException("Contact is null");
                }
            }
            
            this.nodes = ((Set<Contact>)nodes).iterator();
        }
        
        public boolean hasNext() {
            return nodes.hasNext();
        }
        
        public boolean pingNext(Context context, PingResponseHandler responseHandler) 
                throws IOException {
            Contact node = nodes.next();
            
            KUID nodeId = node.getNodeID();
            SocketAddress dst = node.getContactAddress();
            
            RequestMessage request = context.getMessageHelper().createPingRequest(dst);
            return context.getMessageDispatcher().send(nodeId, dst, request, responseHandler);
        }
    }
    
    /**
     * A CollisionPinger sends collision ping requests to a Set of Contacts
     */
    static class CollisionPinger extends ContactPinger {
        
        private final Contact sender;
        
        public CollisionPinger(Context context, Contact sender, Contact node) {
            this(context, sender, Collections.singleton(node));
        }
        
        @SuppressWarnings("unchecked")
        public CollisionPinger(Context context, Contact sender, 
                Set<? extends Contact> nodes) {
            super(nodes);
            
            if (ContextSettings.ASSERT_COLLISION_PING.getValue()) {
                assertCollisionPing(context, sender, nodes);
            }
            
            this.sender = sender;
        }
        
        private void assertCollisionPing(Context context, Contact sender, 
                Set<? extends Contact> nodes) {
            
            KUID localId = context.getLocalNodeID();
            
            if (!ContactUtils.isCollisionPingSender(localId, sender)) {
                throw new IllegalArgumentException(sender 
                        + " is not a valid collision ping Contact");
            }
            
            for (Contact node : nodes) {
                if (!localId.equals(node.getNodeID())) {
                    throw new IllegalArgumentException(node 
                            + " must have the same ID as the local Node ID: " + localId);
                }
            }
        }
        
        @Override
        public boolean pingNext(Context context, PingResponseHandler responseHandler) 
                throws IOException {
            Contact node = nodes.next();
            
            KUID nodeId = node.getNodeID();
            SocketAddress dst = node.getContactAddress();
            
            // Send a collision test ping instead of a regular ping
            RequestMessage request = context.getMessageFactory().createPingRequest(
                    sender, MessageID.createWithSocketAddress(dst));
            
            return context.getMessageDispatcher().send(nodeId, dst, request, responseHandler);
        }
    }

    /**
     * A SocketAddressPinger sends ping requests to a Set of SocketAddresses
     */
    static class SocketAddressPinger implements PingIterator {
        
        private final Iterator<SocketAddress> hosts;
        
        public SocketAddressPinger(SocketAddress host) {
            this(Collections.singleton(host));
        }
        
        @SuppressWarnings("unchecked")
        public SocketAddressPinger(Set<? extends SocketAddress> hosts) {
            if (hosts == null) {
                throw new NullPointerException("Set<SocketAddress> is null");
            }
            
            for (SocketAddress addr : hosts) {
                if (addr == null) {
                    throw new NullPointerException("SocketAddress is null");
                }
            }
            
            this.hosts = ((Set<SocketAddress>)hosts).iterator();
        }
        
        public boolean hasNext() {
            return hosts.hasNext();
        }
        
        public boolean pingNext(Context context, PingResponseHandler responseHandler) 
                throws IOException {
            SocketAddress dst = hosts.next();
            
            RequestMessage request = context.getMessageHelper().createPingRequest(dst);
            return context.getMessageDispatcher().send(null, dst, request, responseHandler);
        }
    }
    
    /**
     * An EntryPinger sends ping requests to a Set of Entry<KUID, SocketAddress>
     */
    static class EntryPinger implements PingIterator {
        
        private final Iterator<Entry<KUID, SocketAddress>> entries;
        
        public EntryPinger(KUID nodeId, SocketAddress address) {
            this(Collections.singleton(
                    new EntryImpl<KUID, SocketAddress>(nodeId, address)));
        }
        
        @SuppressWarnings("unchecked")
        public EntryPinger(Entry<KUID, ? extends SocketAddress> entry) {
            this(Collections.singleton((Entry<KUID, SocketAddress>)entry));
        }
        
        @SuppressWarnings("unchecked")
        public EntryPinger(Set<? extends Entry<KUID, ? extends SocketAddress>> entries) {
            if (entries == null) {
                throw new NullPointerException("Set<Entry<KUID, SocketAddress>> is null");
            }
            
            for (Entry<KUID, ? extends SocketAddress> entry : entries) {
                if (entry == null) {
                    throw new NullPointerException("Entry<KUID, SocketAddress> is null");
                }
                
                if (entry.getValue() == null) {
                    throw new NullPointerException("SocketAddress is null");
                }
            }
            
            this.entries = ((Set<Entry<KUID, SocketAddress>>)entries).iterator();
        }
        
        public boolean hasNext() {
            return entries.hasNext();
        }
        
        public boolean pingNext(Context context, PingResponseHandler responseHandler) 
                throws IOException {
            
            Entry<KUID, SocketAddress> entry = entries.next();
            
            KUID nodeId = entry.getKey();
            SocketAddress dst = entry.getValue();
            
            RequestMessage request = context.getMessageHelper().createPingRequest(dst);
            return context.getMessageDispatcher().send(nodeId, dst, request, responseHandler);
        }
    }
}
