package com.limegroup.gnutella.dht.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.EventDispatcher;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.routing.RouteTable;

/**
 * Controlls an active DHT node.
 * 
 * Active nodes load and persist DHT data, as they keep the same node ID 
 * over multiple sessions and can therefore re-use their local routing table
 * and local database. 
 * 
 */
class ActiveDHTNodeController extends AbstractDHTController {
    
    /**
     * The file to persist this Mojito DHT
     */
    private static final File FILE = new File(CommonUtils.getUserSettingsDir(), "active.mojito");
    
    public ActiveDHTNodeController(int vendor, int version, 
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher) {
        super(dispatcher);
        
        MojitoDHT dht = MojitoFactory.createDHT("ActiveMojitoDHT", vendor, version);
        
        if (DHTSettings.PERSIST_DHT.getValue() && 
                FILE.exists() && FILE.isFile()) {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(
                        new BufferedInputStream(
                            new FileInputStream(FILE)));
                
                RouteTable routeTable = (RouteTable)in.readObject();
                Database database = (Database)in.readObject();
                
                synchronized (dht) {
                    dht.setRouteTable(routeTable);
                    dht.setDatabase(database);
                }
                
            } catch (FileNotFoundException e) {
                LOG.error("FileNotFoundException", e);
            } catch (ClassNotFoundException e) {
                LOG.error("ClassNotFoundException", e);
            } catch (IOException e) {
                LOG.error("IOException", e);
            } finally {
                IOUtils.close(in);
            }
        }
        
        setMojitoDHT(dht);
    }

    /**
     * @see AbstractDHTController.start
     * 
     * Adds the following precondition to start an active DHT node: 
     * We are not an ultrapeer while excluding ultrapeers from the active network
     * 
     */
    @Override
    public void start() {
        //if we want to connect actively, we should be active DHT capable
        if (!DHTSettings.FORCE_DHT_CONNECT.getValue() 
                && !DHTSettings.ACTIVE_DHT_CAPABLE.getValue()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Cannot initialize ACTIVE DHT - node is not DHT active capable");
            }
            return;
        }
        
        super.start();
    }

    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }
        
        super.stop();
        
        //Notify our ultrapeers that we disconnected
        sendUpdatedCapabilities();
        
        if (DHTSettings.PERSIST_DHT.getValue()) {
            ObjectOutputStream out = null;
            try {
                out = new ObjectOutputStream(
                        new BufferedOutputStream(
                            new FileOutputStream(FILE)));
                MojitoDHT dht = getMojitoDHT();
                synchronized (dht) {
                    out.writeObject(dht.getRouteTable());
                    out.writeObject(dht.getDatabase());
                }
                out.flush();
            } catch (IOException err) {
                LOG.error("IOException", err);
            } finally {
                IOUtils.close(out);
            }
        }
    }
    
    public boolean isActiveNode() {
        return true;
    }

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        //handle connection specific events
        Connection c = evt.getConnection();
        if(c == null) {
            return;
        }
        
        String host = c.getAddress();
        int port = c.getPort();

        if(evt.isConnectionCapabilitiesEvent()){
            
            if(c.remostHostIsPassiveDHTNode() > -1) {
                
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Connection is passive dht node: "+ c);
                }
                addPassiveDHTNode(new InetSocketAddress(host, port));
                
            } else if(c.remostHostIsActiveDHTNode() > -1) {
                
                if(DHTSettings.EXCLUDE_ULTRAPEERS.getValue()) {
                    return;
                } 
                addActiveDHTNode(new InetSocketAddress(host, port));
            } 
        }
        
    }

    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        if(!isRunning() || !getMojitoDHT().isBootstrapped()) {
            return Collections.emptyList();
        }
        //return our MRS nodes. We should be first in the list
        return getMRSNodes(maxNodes, false);
    }
}