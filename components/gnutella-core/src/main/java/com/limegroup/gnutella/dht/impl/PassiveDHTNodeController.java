package com.limegroup.gnutella.dht.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.dht.LimeDHTRoutingTable;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.util.BucketUtils;

class PassiveDHTNodeController extends AbstractDHTController{
    
    private LimeDHTRoutingTable limeDHTRouteTable;
    
    private static final Log LOG = LogFactory.getLog(PassiveDHTNodeController.class);
    
    /**
     * The file to persist the list of host
     */
    private static final File FILE = new File(CommonUtils.getUserSettingsDir(), "dhtnodes.dat");

    @Override
    public void init() {
        dht = new MojitoDHT("PassiveMojitoDHT", true);
        limeDHTRouteTable = (LimeDHTRoutingTable) dht.setRoutingTable(LimeDHTRoutingTable.class);
        setLimeMessageDispatcher();
        //load the small list of MRS nodes for bootstrap
        try {
            if (FILE.exists() && FILE.isFile()) {
                FileInputStream in = new FileInputStream(FILE);
                ObjectInputStream oii = new ObjectInputStream(in);
                Contact node;
                while((node = (Contact)oii.readObject()) != null){
                    addBootstrapHost(node.getContactAddress());
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
    
    private synchronized void addLeafDHTNode(String host, int port) {
        if(!isRunning()) {
            return;
        }
        
        InetSocketAddress addr = new InetSocketAddress(host, port);
        if(isWaiting() || isBootstrappingFromRT()) {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Adding host: "+addr+" to leaf dht nodes");
            }
            
            addBootstrapHost(addr);
        } else {
            limeDHTRouteTable.addLeafDHTNode(addr);
        }
    }
    
    private synchronized void removeLeafDHTNode(String host, int port) {
        if(!isRunning()) {
            return;
        }
        
        IpPort removed = limeDHTRouteTable.removeLeafDHTNode(host, port);

        if(LOG.isDebugEnabled() && removed != null) {
            LOG.debug("Removed host: "+removed+" from leaf dht nodes");
        }
    }

    @Override
    protected void sendUpdatedCapabilities() {
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
        if(!isRunning() || isWaiting()) {
            return Collections.emptyList();
        }
        
        //IF we have any DHT leaves we return them directly
        if(limeDHTRouteTable.hasDHTLeaves()) {
            List<IpPort> dhtLeaves = new ArrayList<IpPort>(limeDHTRouteTable.getDHTLeaves());
            //size will be > 0
            return dhtLeaves.subList(0,Math.min(maxNodes, dhtLeaves.size()));
        }
        
        System.out.append("rout table:" + limeDHTRouteTable);
        //ELSE return the MRS node of our routing table
        List<Contact> nodes = BucketUtils.getMostRecentlySeenContacts(
                limeDHTRouteTable.getLiveContacts(), maxNodes + 1); //it will add the local node!
        
        KUID localNode = dht.getLocalNodeID();
        List<IpPort> ipps = new ArrayList<IpPort>();
        for(Contact cn : nodes) {
            if(cn.getNodeID().equals(localNode)) {
                continue;
            }
            ipps.add(new IpPortContactNode(cn));
        }
        
        return ipps;
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
            removeLeafDHTNode( host , port );
            
        } else if(evt.isConnectionVendoredEvent()){
            
            if(c.remostHostIsDHTNode() > -1) {
                addLeafDHTNode( host , port );
            } else {
                removeLeafDHTNode( host , port );
            }
        } 
    }
}