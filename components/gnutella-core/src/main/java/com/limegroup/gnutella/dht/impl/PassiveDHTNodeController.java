package com.limegroup.gnutella.dht.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.statistics.DHTStatsFactory;
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
 * 
 *   
 */
class PassiveDHTNodeController extends AbstractDHTController{
    
    private LimeDHTRouteTable limeDHTRouteTable;
    
    /**
     * The file to persist the list of host
     */
    private static final File FILE = new File(CommonUtils.getUserSettingsDir(), "dhtnodes.dat");

    @Override
    public void init() {
        
        DHTStatsFactory.clear();
        dht = MojitoFactory.createFirewalledDHT("PassiveMojitoDHT");
        
        limeDHTRouteTable = new LimeDHTRouteTable(dht);
        dht.setRouteTable(limeDHTRouteTable);
        
        setLimeMessageDispatcher();
        //load the small list of MRS nodes for bootstrap
        try {
            if (FILE.exists() && FILE.isFile()) {
                FileInputStream in = new FileInputStream(FILE);
                ObjectInputStream ois = new ObjectInputStream(in);
                Contact node = null;
                while((node = (Contact)ois.readObject()) != null){
                    limeDHTRouteTable.add(node);
                }
            }
        } catch (ClassNotFoundException e) {
            LOG.error("ClassNotFoundException", e);
        } catch (FileNotFoundException e) {
            LOG.error("FileNotFoundException", e);
        } catch (IOException e) {
            LOG.error("IOException", e);
        }
    }
    
    /**
     * Adds a leaf DHT node to the list and then tries to bootstrap off it 
     * if we are not bootstrapped yet.
     * Note: This method makes sure the DHT is running already, as adding a node
     * as a leaf involves sending it a DHT ping (in order to get its KUID).
     * 
     */
    protected synchronized void addLeafDHTNode(String host, int port) {
        if(!isRunning()) {
            return;
        }
        
        InetSocketAddress addr = new InetSocketAddress(host, port);
        //add to bootstrap nodes if we need to.
        addDHTNode(addr, false);
        //add to our DHT leaves
        if(LOG.isDebugEnabled()) {
            LOG.debug("Adding host: "+addr+" to leaf dht nodes");
        }
        limeDHTRouteTable.addLeafDHTNode(host, port);
    }
    
    protected synchronized SocketAddress removeLeafDHTNode(String host, int port) {
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
    public void sendUpdatedCapabilities() {
        // DO NOTHING
    }

    @Override
    public synchronized void stop() {
        //TODO: here, save a small list of MRS nodes for next bootstrap
        super.stop();
        
        try {
            FileOutputStream out = new FileOutputStream(FILE);
            ObjectOutputStream oos = new ObjectOutputStream(out);
            List<Contact> contacts = limeDHTRouteTable.getLiveContacts(); 
            if(contacts.size() < 2) {
                //delete the existing RT so we start next session from scratch
                out.close();
                FILE.delete();
                return;
            }
            
            //sort by MRS
            BucketUtils.sort(contacts);
            //only save some nodes
            contacts = contacts.subList(0, 
                    Math.min(DHTSettings.NUM_PERSISTED_NODES.getValue(), contacts.size()));
            KUID localNodeID = dht.getLocalNodeID();
            for(Contact node : contacts) {
                if(!node.getNodeID().equals(localNodeID)) {
                    oos.writeObject(node);
                }
            }
            //Terminator
            oos.writeObject(null); 
            out.close();
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    };
    
    public boolean isActiveNode() {
        return false;
    }

    @Override
    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        if(!isRunning() || !dht.isBootstrapped()) {
            return Collections.emptyList();
        }
        
        //This should return the leafs first (they have the highest timestamp)
        //TODO: Although a passive node does not have accurate info in its RT 
        //(except for direct leafs), we still return nodes. Maybe be stricter here?
        return getMRSNodes(maxNodes, true);
    }
    
    public void handleLifecycleEvent(LifecycleEvent evt) {
        //handle network connections - disconnections
        super.handleLifecycleEvent(evt);
        
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
                    LOG.debug("Connection is active capable: "+ c);
                }
                addLeafDHTNode( host , port );
            } else {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Connection is NOT active capable: "+ c);
                }
                removeLeafDHTNode( host , port );
            }
        } 
    }
}