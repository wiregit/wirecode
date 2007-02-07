package com.limegroup.gnutella.dht.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.SecureInputStream;
import org.limewire.io.SecureOutputStream;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.util.ContactUtils;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.EventDispatcher;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Controlls a passive DHT node. 
 * 
 * As passive nodes are Gnutella ultrapeers, the passive node controller also
 * maintains the list of this ultrapeer's leafs in a separate list. 
 * These leaves are this node's most accurate knowledge of the DHT, as leaf 
 * connections are statefull TCP, and they are therefore propagated in priority
 * in the Gnutella network.
 * 
 * Persistence is implemented in order to be able to bootstrap next session 
 * by saving a few (DHTSettings.NUM_PERSISTED_NODES) MRS nodes. It is not necessary
 * to persist the entire DHT, because we don't want to keep the same nodeID at the next 
 * session, and accuracy of the contacts in the RT is not guaranteed when a node 
 * is passive (as it does not get contacted by the DHT).   
 */
class PassiveDHTNodeController extends AbstractDHTController{
    
    /**
     * The file to persist the list of host
     */
    private static final File FILE = new File(LimeWireUtils.getUserSettingsDir(), "passive.mojito");
    
    /**
     * A RouteTable for passive Nodes
     */
    private PassiveDHTNodeRouteTable limeDHTRouteTable;
    
    public PassiveDHTNodeController(Vendor vendor, Version version, 
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher) {
        super(vendor, version, dispatcher);
    }
    
    @Override
    protected MojitoDHT createMojitoDHT(Vendor vendor, Version version) {
        MojitoDHT dht = MojitoFactory.createFirewalledDHT("PassiveMojitoDHT", vendor, version);
        
        limeDHTRouteTable = new PassiveDHTNodeRouteTable(dht);
        dht.setRouteTable(limeDHTRouteTable);
        
        // We're an Ultrapeer and there are some special 
        // republishing rules for our firewalled leaf Nodes.
        dht.setRepublishManager(new PassiveDHTNodeRepublishManager());
        
        // Load the small list of MRS Nodes for bootstrap
        if (FILE.exists() && FILE.isFile()) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(
                            new BufferedInputStream(
                                new SecureInputStream(
                                    new FileInputStream(FILE))));
                
                Contact node = null;
                while((node = (Contact)ois.readObject()) != null){
                    limeDHTRouteTable.add(node);
                }
            } catch (Throwable ignored) {
            } finally {
                IOUtils.close(ois);
            }
        }
        
        return dht;
    }


    /**
     * This method first adds the given host to the list of bootstrap nodes and 
     * then adds it to this passive node's routing table.
     * 
     * Note: This method makes sure the DHT is running already, as adding a node
     * as a leaf involves sending it a DHT ping (in order to get its KUID).
     * 
     */
    protected void addLeafDHTNode(String host, int port) {
        if(!isRunning()) {
            return;
        }
        
        SocketAddress addr = new InetSocketAddress(host, port);
        //add to bootstrap nodes if we need to.
        addActiveDHTNode(addr, false);
        //add to our DHT leaves
        if(LOG.isDebugEnabled()) {
            LOG.debug("Adding host: "+addr+" to leaf dht nodes");
        }
        limeDHTRouteTable.addLeafDHTNode(host, port);
    }
    
    protected SocketAddress removeLeafDHTNode(String host, int port) {
        if(!isRunning()) {
            return null;
        }
        
        SocketAddress removed = limeDHTRouteTable.removeLeafDHTNode(host, port);

        if(LOG.isDebugEnabled() && removed != null) {
            LOG.debug("Removed host: "+removed+" from leaf dht nodes");
        }
        return removed;
    }

    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }
        
        super.stop();
        
        // Delete the previous file
        if (FILE.exists()) {
            FILE.delete();
        }
        
        List<Contact> contacts = limeDHTRouteTable.getActiveContacts(); 
        if (contacts.size() >= 2) {
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(
                            new BufferedOutputStream(
                                new SecureOutputStream(
                                    new FileOutputStream(FILE))));
                
                // Sort by MRS and save only some Nodes
                contacts = ContactUtils.sort(contacts, DHTSettings.NUM_PERSISTED_NODES.getValue());
                
                KUID localNodeID = getMojitoDHT().getLocalNodeID();
                for(Contact node : contacts) {
                    if(!node.getNodeID().equals(localNodeID)) {
                        oos.writeObject(node);
                    }
                }
                
                // EOF Terminator
                oos.writeObject(null);
                oos.flush();
                
            } catch (IOException ignored) {
            } finally {
                IOUtils.close(oos);
            }
        }
    }
    
    public boolean isActiveNode() {
        return false;
    }

    /**
     * This method return this passive node's leafs first (they have the highest timestamp)
     * 
     * Note: Although a passive node does not have accurate info in its RT 
     * (except for direct leafs), we still return nodes. 
     */
    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        if(!isRunning() || !getMojitoDHT().isBootstrapped()) {
            return Collections.emptyList();
        }
        
        return getMRSNodes(maxNodes, true);
    }
    
    /**
     * Handle connection-specific lifecycle events only. 
     */
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        //handle connection specific events
        Connection c = evt.getConnection();
        if( c == null) {
            return;
        }
        
        String host = c.getAddress();
        int port = c.getPort();
        
        if(evt.isConnectionClosedEvent()) {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Got a connection closed event for connection: "+ c);
            }
            
            removeLeafDHTNode( host , port );
            
        } else if(evt.isConnectionCapabilitiesEvent()){
            
            if(c.remostHostIsActiveDHTNode() > -1) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Connection is active dht node: "+ c);
                }
                addLeafDHTNode( host , port );
            } else if(c.remostHostIsPassiveDHTNode() > -1) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Connection is passive dht node: "+ c);
                }
                addPassiveDHTNode(new InetSocketAddress(host, port));
            } else {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Connection is node not connected to the DHT network: "+ c);
                }
                removeLeafDHTNode( host , port );
            }
        } 
    }
}
