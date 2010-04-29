package org.limewire.mojito2.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.collection.CollectionUtils;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.StoreSettings;
import org.limewire.mojito2.message.Message;
import org.limewire.mojito2.message.SecurityTokenProvider;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.RouteTable.SelectMode;
import org.limewire.security.SecurityToken;

public class DefaultStoreForward implements StoreForward {

    private static final Log LOG 
        = LogFactory.getLog(DefaultStoreForward.class);
    
    @InspectablePrimitive(value = "Store-Forward Count")
    private static final AtomicInteger STORE_FORWARD_VALUE = new AtomicInteger();
    
    @InspectablePrimitive(value = "Store-Drop Count")
    private static final AtomicInteger STORE_DROP_VALUE = new AtomicInteger();
    
    private static enum Operation {
        // Do nothing
        NOTHING,
        
        // Forward value
        FORWARD,
        
        // Delete value
        DELETE;
    }
    
    private final RouteTable routeTable;
    
    private final Database database;
    
    private final Provider provider = null;
    
    public DefaultStoreForward(RouteTable routeTable, Database database) {
        this.routeTable = routeTable;
        this.database = database;
    }
    
    private boolean isLocalNode(Contact contact) {
        return routeTable.getLocalNode().equals(contact);
    }
    
    private KUID getLocalNodeID() {
        return routeTable.getLocalNode().getNodeID();
    }
    
    @Override
    public void process(Contact node, Message message) {
        KUID nodeId = node.getNodeID();
        
        if (StoreSettings.STORE_FORWARD_ENABLED.getValue()) {
            // Only do store forward if it is a new node in our routing table 
            // (we are (re)connecting to the network) or a node that is reconnecting
            Contact existing = routeTable.get(nodeId);
            
            if (existing == null
                    || existing.isDead()
                    || existing.getInstanceID() != node.getInstanceID()) {
                
                // Store forward only if we're bootstrapped
                if (provider != null && provider.isBootstrapped()) {
                    int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
                    //we select the 2*k closest nodes in order to also check those values
                    //where the local node is part of the k closest to the value but not part
                    //of the k closest to the new joining node.
                    Collection<Contact> nodes = routeTable.select(nodeId, 2*k, SelectMode.ALL);
                    
                    // Are we one of the K nearest Nodes to the contact?
                    if (containsNodeID(nodes, getLocalNodeID())) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Node " + node + " is new or has changed his instanceID, will check for store forward!");   
                        }
                        
                        forwardOrRemoveValues(node, existing, message);
                    }
                }
            }
        }
    }
    
    /**
     * This method depends on addLiveContactInfo(...) and does two things.
     * It either forwards or removes a DHTValue it from the local Database.
     * For details see Kademlia spec!
     */
    private void forwardOrRemoveValues(Contact node, 
            Contact existing, Message message) {
        
        List<DHTValueEntity> valuesToForward = new ArrayList<DHTValueEntity>();
        
        synchronized (database) {
            for(KUID primaryKey : database.keySet()) {
                
                Operation op = getOperation(node, existing, primaryKey);
                
                if (LOG.isDebugEnabled())
                    LOG.debug("node: " + node + ", existing: " + existing + ", operation: " + op);
                
                if (op.equals(Operation.FORWARD)) {
                    Map<KUID, DHTValueEntity> bag = database.get(primaryKey);
                    valuesToForward.addAll(bag.values());
                    STORE_FORWARD_VALUE.incrementAndGet();
                    
                } else if (op.equals(Operation.DELETE)
                        && DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.getValue()) {
                    Map<KUID, DHTValueEntity> bag = database.get(primaryKey);
                    for (DHTValueEntity entity : bag.values()) {
                        //System.out.println("REMOVING: " + entity + "\n");
                        database.remove(entity.getPrimaryKey(), entity.getSecondaryKey());
                    }
                    STORE_DROP_VALUE.incrementAndGet();
                }
            }
        }
        
        if (!valuesToForward.isEmpty()) {
            SecurityToken securityToken = null;
            if (message instanceof SecurityTokenProvider) {
                securityToken = ((SecurityTokenProvider)message).getSecurityToken();
                
                if (securityToken == null
                        && StoreSettings.STORE_REQUIRES_SECURITY_TOKEN.getValue()) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info(node + " sent us a null SecurityToken");
                    }
                    return;
                }
            }
            
            provider.store(node, securityToken, valuesToForward);
        }
    }
    
    /**
     * Returns whether or not the local Node is in the given List
     */
    private static boolean containsNodeID(Collection<Contact> nodes, KUID id) {
        for (Contact node : nodes) {
            if (id.equals(node.getNodeID())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines whether to remove, forward or to do nothing with the
     * value that is associated with the given valueId.
     */
    private Operation getOperation(Contact node, 
            Contact existing, KUID valueId) {
        
        // To avoid redundant STORE forward, a node only transfers a value 
        // if it is the closest to the key or if its ID is closer than any 
        // other ID (except the new closest one of course)
        // TODO: maybe relax this a little bit: what if we're not the closest 
        // and the closest is stale?
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        List<Contact> nodes = CollectionUtils.toList(
                routeTable.select(valueId, k, SelectMode.ALL));
        Contact closest = nodes.get(0);
        Contact furthest = nodes.get(nodes.size()-1);
        
        if (LOG.isDebugEnabled()) {
            LOG.debugf("node: {0}, existing: {1}, close nodes: {2}", node, existing, nodes);
        }
        
//        StringBuilder sb = new StringBuilder();
//        sb.append("ME: "+context.getLocalNode()+"\n");
//        sb.append("Them: "+node).append("\n");
//        sb.append("RT nearest: " + closest).append("\n");
//        sb.append("RT furthest: " + furthest).append("\n");
//        sb.append(CollectionUtils.toString(nodes)).append("\n");
        
        // We store forward if:
        // #1 We're the nearest Node of the k-closest Nodes to
        //    the given valueId
        //
        // #2 We're the second nearest of the k-closest Nodes to
        //    the given valueId AND the other Node is the nearest.
        //    In other words it changed its instance ID 'cause it
        //    was offline for a short period of time or whatsoever.
        //    (see also pre-condition(s) from where we're calling
        //    this method)
        //
        // The first condition applies if the Node is new
        // and we're the closest Node. The second condition
        // applies if the Node has changed it's instanceId.
        // That means we're the second closest and since
        // the other Node has changed its instanceId we must
        // re-send the values
        if (isLocalNode(closest)
                || (node.equals(closest)
                        && nodes.size() > 1
                        && isLocalNode(nodes.get(1)))) {
            
            KUID nodeId = node.getNodeID();
            KUID furthestId = furthest.getNodeID();
            
            // #3 The other Node must be equal to the furthest Node
            //    or better
            if (nodeId.equals(furthestId) 
                    || nodeId.isNearerTo(valueId, furthestId)) {
        
//                sb.append("CONDITION B (FORWARD)").append("\n");
//                sb.append("Local (from): " + context.getLocalNode()).append("\n");
//                sb.append("Remote (to): " + node).append("\n");
//                sb.append(CollectionUtils.toString(nodes)).append("\n");
//                System.out.println(sb.toString());
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Node " + node + " is now close enough to a value and we are responsible for xfer");   
                }
                
                return Operation.FORWARD;
            }
        
        // We remove a value if:
        // #1 The value is stored at k Nodes 
        //    (i.e. the total number of Nodes in the DHT
        //     is equal or greater than k. If the DHT has
        //     less than k Nodes then there's no reason to
        //     remove a value)
        //
        // #2 This Node is the furthest of the k-closest Nodes
        //
        // #3 The new Node isn't in our RouteTable yet. That means
        //    adding it will push this Node out of the club of the
        //    k-closest Nodes and makes it the (k+1)-closest Node.
        //    
        // #4 The new Node is nearer to the given valueId then
            //    the furthest away Node (we).
        } else if (nodes.size() >= k 
                && isLocalNode(furthest) 
                && (existing == null || existing.isDead())) {
            
            KUID nodeId = node.getNodeID();
            KUID furthestId = furthest.getNodeID();
                
            if (nodeId.isNearerTo(valueId, furthestId)) {
//                sb.append("CONDITION C").append("\n");
//                sb.append("ME:").append(context.getLocalNode()).append("\n");
//                sb.append("VALUE:").append(valueId).append("\n");
//                sb.append("NODE:").append(node).append("\n");
//                sb.append(CollectionUtils.toString(nodes)).append("\n");
//                System.out.println(sb.toString());
                
                return Operation.DELETE;
            }
        }
        
        return Operation.NOTHING;
    }
    
    private static interface Provider {
        
        public boolean isBootstrapped();
        
        public void store(Contact contact, SecurityToken securityToken, 
                Collection<? extends DHTValueEntity> values);
    }
}
