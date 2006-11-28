package com.limegroup.gnutella.dht.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.statistics.DHTStatsManager;
import com.limegroup.mojito.util.BucketUtils;

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
    
    private PassiveDHTNodeRouteTable limeDHTRouteTable;
    
    /**
     * The file to persist the list of host
     */
    private static final File FILE = new File(CommonUtils.getUserSettingsDir(), "passive.mojito");
    
    public PassiveDHTNodeController(int vendor, int version) {

        DHTStatsManager.clear();
        MojitoDHT dht = MojitoFactory.createFirewalledDHT("PassiveMojitoDHT", vendor, version);
        
        limeDHTRouteTable = new PassiveDHTNodeRouteTable(dht);
        dht.setRouteTable(limeDHTRouteTable);
        setMojitoDHT(dht);
        
        // Load the small list of MRS Nodes for bootstrap
        if (FILE.exists() && FILE.isFile()) {
            InputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(FILE));
                ObjectInputStream ois = new ObjectInputStream(in);
                Contact node = null;
                while((node = (Contact)ois.readObject()) != null){
                    limeDHTRouteTable.add(node);
                }
            } catch (ClassNotFoundException e) {
                LOG.error("ClassNotFoundException", e);
            } catch (FileNotFoundException e) {
                LOG.error("FileNotFoundException", e);
            } catch (IOException e) {
                LOG.error("IOException", e);
            } finally {
                IOUtils.close(in);
            }
        }
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
        
        InetSocketAddress addr = new InetSocketAddress(host, port);
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
        super.stop();
        
        // Delete the previous file
        if (FILE.exists()) {
            FILE.delete();
        }
        
        List<Contact> contacts = limeDHTRouteTable.getActiveContacts(); 
        if (contacts.size() >= 2) {
            OutputStream out = null;
            try {
                out = new BufferedOutputStream(new FileOutputStream(FILE));
                ObjectOutputStream oos = new ObjectOutputStream(out);
                
                // Sort by MRS
                contacts = BucketUtils.sort(contacts);
                
                // Save only some Nodes
                contacts = contacts.subList(0, 
                        Math.min(DHTSettings.NUM_PERSISTED_NODES.getValue(), contacts.size()));
                KUID localNodeID = getMojitoDHT().getLocalNodeID();
                for(Contact node : contacts) {
                    if(!node.getNodeID().equals(localNodeID)) {
                        oos.writeObject(node);
                    }
                }
                
                // EOF Terminator
                oos.writeObject(null);
                
            } catch (IOException err) {
                LOG.error("IOException", err);
            } finally {
                IOUtils.close(out);
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
    public void handleConnectionLifecycleEvent(LifecycleEvent evt) {
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
                    LOG.debug("Connection is DHT node not connected to the network: "+ c);
                }
                removeLeafDHTNode( host , port );
            }
        } 
    }
}