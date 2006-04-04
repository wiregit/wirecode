/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package de.kapsi.net.kademlia.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.db.Database;
import de.kapsi.net.kademlia.db.KeyValueCollection;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.routing.RoutingTable;
import de.kapsi.net.kademlia.security.QueryKey;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.settings.LookupSettings;
import de.kapsi.net.kademlia.settings.NetworkSettings;
import de.kapsi.net.kademlia.util.FixedSizeHashMap;
import de.kapsi.net.kademlia.util.NetworkUtils;

/**
 * The DefaultMessageHandler performs basic Kademlia RouteTable 
 * update operations. That means adding new Nodes if RouteTable 
 * is not full, updating the last seen time stamp of Nodes and 
 * so forth.
 */
public class DefaultMessageHandler extends MessageHandler 
        implements RequestHandler, ResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(DefaultMessageHandler.class);
    
    private long timeout = LookupSettings.getTimeout();
    
    private Map loopLock = new FixedSizeHashMap(16);
    
    public DefaultMessageHandler(Context context) {
        super(context);
    }
    
    public long timeout() {
        return timeout;
    }

    public void handleResponse(KUID nodeId, SocketAddress src, 
            Message message, long time) throws IOException {
        
        ContactNode node = getRouteTable().get(nodeId, true);
        if (node == null) {
            addLiveContactInfo(nodeId, src, message);
        } else {
            updateContactInfo(node, nodeId, src, message);
        }
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, 
            long time) throws IOException {
        RoutingTable routeTable = getRouteTable();
        routeTable.handleFailure(nodeId);
    }

    public void handleRequest(KUID nodeId, SocketAddress src, 
            Message message) throws IOException {
        
        ContactNode node = getRouteTable().get(nodeId, true);
        if (node == null) {
            addLiveContactInfo(nodeId, src, message);
        } else {
            updateContactInfo(node, nodeId, src, message);
        }
    }
    
    private void addLiveContactInfo(KUID nodeId, SocketAddress src, 
            Message message) throws IOException {
        
        RoutingTable routeTable = getRouteTable();
        
        ContactNode node = new ContactNode(nodeId, src);
        routeTable.add(node, true);
        
        int k = KademliaSettings.getReplicationParameter();
        
        //are we one of the K closest nodes to the contact?
        List closestNodes = routeTable.select(nodeId, k, false, false);
        
        if (closestNodes.contains(context.getLocalNode())) {
            final List toStore = new ArrayList();
            
            Database database = context.getDatabase();
            synchronized(database) {
                Collection keyValues = database.getAllCollections();
                for (Iterator iter = keyValues.iterator(); iter.hasNext(); ) {
                    KeyValueCollection c = (KeyValueCollection)iter.next();
                    
                    //To avoid redundant STORE forward, a node only transfers a value if it is the closest to the key
                    //or if it's ID is closer than any other ID (except the new closest one of course)
                    //TODO: maybe relax this a little bit: what if we're not the closest and the closest is stale?
                    List closestNodesToKey = routeTable.select(c.getKey(), k, false, false);
                    ContactNode closest = (ContactNode)closestNodesToKey.get(0);
                    if (closest.equals(context.getLocalNode())   
                            || ((node.equals(closest)|| node.getNodeID().isCloser(closest.getNodeID(), c.getKey()))
                                    && (closestNodesToKey.size() > 1)
                                    && closestNodesToKey.get(1).equals(context.getLocalNode()))) {
                        
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Node "+node+" is now close enough to a value and we are responsible for xfer");   
                        }
                        toStore.addAll(c);
                    }
                }
            }
            
            if (!toStore.isEmpty()) {
                ResponseHandler handler = new StoreForwardResponseHandler(context, toStore);
                RequestMessage request = context.getMessageFactory().createFindNodeRequest(nodeId);
                context.getMessageDispatcher().send(nodeId, src, request, handler);
            }
        }
    }
    
    private void updateContactInfo(ContactNode currentContact, 
            KUID nodeId, SocketAddress src, Message message) throws IOException {
        
        // Technically, this shouldn't be possible since MessageDispatcher
        // already takes care of it! We cannot send nor receive Messages
        // from Nodes that have the same Node ID as we do. This is just
        // for the case somebody is screwing around with the MessageDispatcher!
        if (nodeId.equals(context.getLocalNodeID())) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot update local contact info");
            }
            return;
        }
        
        // Same Address? OK, mark it as alive if it 
        // isn't aleardy
        if (currentContact.getSocketAddress().equals(src)) {
            context.getRouteTable().add(currentContact, true);
            return;
        }
        
        // Huh? The addresses are not equal but both belong
        // obviously to this local machine!? There isn't much
        // we can do. Set it to the new address and hope it 
        // doesn't use a different NIF everytime...
        if (NetworkUtils.isLocalAddress(src)
                && NetworkUtils.isLocalAddress(currentContact.getSocketAddress())) {
            // TODO what's better?
            context.getRouteTable().add(new ContactNode(nodeId, src), true);
            //node.setSocketAddress(src);
            return;
        }
        
        // If a Host has multiple IPs (see also above case) then
        // we may create an infinite loop if boths ends think
        // the other end is trying to spoof its Node ID! Make sure
        // we're not creating a such loop.
        if (loopLock.containsKey(nodeId)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Spoof check already in progress for " + currentContact);
            }
            return;
        }
        
        // Kick off the spoof check. Ping the current contact and
        // if it responds then interpret it as an attempt to spoof
        // the node ID and if it doesn't then it is obviously dead.
        ResponseHandler handler 
            = new SpoofCheckHandler(context, 
                currentContact, new ContactNode(nodeId, src));
        
        doSpoofCheck(currentContact, handler);
    }
    
    private void doSpoofCheck(ContactNode contact, ResponseHandler handler) throws IOException {
        RequestMessage request = context.getMessageFactory().createPingRequest();
        context.getMessageDispatcher().send(contact, request, handler);
        loopLock.put(contact.getNodeID(), handler);
    }
    
    /**
     * Handles a spoof check where we're trying to figure out
     * wheather or not a Node is trying to spoof its Node ID.
     */
    private class SpoofCheckHandler extends AbstractResponseHandler {
        
        private boolean done = false;
        
        private int errors = 0;
        
        private ContactNode currentContact;
        private ContactNode newContact;
        
        public SpoofCheckHandler(Context context, ContactNode currentContact, ContactNode newContact) {
            super(context);
            
            this.currentContact = currentContact;
            this.newContact = newContact;
        }

        public void handleResponse(KUID nodeId, SocketAddress src, 
                Message message, long time) throws IOException {
            
            if (done) {
                return;
            }
            
            loopLock.remove(nodeId);
            done = true;
            
            if (LOG.isWarnEnabled()) {
                LOG.warn(newContact + " is trying to spoof its NodeID. " 
                        + ContactNode.toString(nodeId, src) 
                        + " responded in " + time + " ms");
            }
            
            // Do nothing else! DefaultMessageHandler takes
            // care of everything else!
        }

        public void handleTimeout(KUID nodeId, SocketAddress dst, 
                long time) throws IOException {
            
            if (done) {
                return;
            }
            
            // Try at least x-times before giving up!
            if (++errors >= NetworkSettings.getMaxErrors()) {
                
                loopLock.remove(nodeId);
                done = true;
                
                // The current contact is obviously not responding
                if (LOG.isInfoEnabled()) {
                    LOG.info(currentContact + " does not respond! Replacing it with " + newContact);
                }
                
                // TODO this should be maybe part of route tables internal logic?
                ContactNode node = context.getRouteTable().get(nodeId);
                if (node == null) {
                    node = newContact;
                } else {
                    node.setSocketAddress(newContact.getSocketAddress());
                }
                
                context.getRouteTable().add(node, true);
                return;
            }
            
            doSpoofCheck(currentContact, this);
        }
    }

    /**
     * Handles Store-Forward response. We're actually sending our
     * Target Node a lookup for its own Node ID and it will tell us
     * the QueryKey we need to store a KeyValue.
     */
    private static class StoreForwardResponseHandler extends AbstractResponseHandler {

        private List keyValues;
        
        private boolean done = false;
        private int errors = 0;
        
        public StoreForwardResponseHandler(Context context, List keyValues) {
            super(context);
            this.keyValues = keyValues;
        }
        
        public void handleResponse(KUID nodeId, SocketAddress src, 
                Message message, long time) throws IOException {
            
            if (done) {
                return;
            }
            
            FindNodeResponse response = (FindNodeResponse)message;
            
            Collection values = response.getValues();
            for(Iterator it = values.iterator(); it.hasNext(); ) {
                // We did a FIND_NODE lookup use the info
                // to fill our routing table
                ContactNode node = (ContactNode)it.next();
                context.getRouteTable().add(node, false);
            }
            
            QueryKey queryKey = response.getQueryKey();
            context.store(new ContactNode(nodeId, src), queryKey, keyValues);
            done = true;
        }

        public void handleTimeout(KUID nodeId, SocketAddress dst, long time) 
                throws IOException {
            
            if (done) {
                return;
            }
            
            if (++errors >= NetworkSettings.getMaxErrors()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Max number of errors has occured. Giving up!");
                }
                return;
            }
            
            RequestMessage request = context.getMessageFactory().createFindNodeRequest(nodeId);
            context.getMessageDispatcher().send(nodeId, dst, request, this);
        }
    }
}
