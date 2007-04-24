package com.limegroup.gnutella.dht;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.SecureInputStream;
import org.limewire.io.SecureOutputStream;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * Controlls an active DHT node.
 * 
 * Active nodes load and persist DHT data, as they keep the same node ID 
 * over multiple sessions and can therefore re-use their local routing table
 * and local database. 
 * 
 */
public class ActiveDHTNodeController extends AbstractDHTController {
    
    /**
     * The file to persist this Mojito DHT
     */
    private static final File FILE = new File(CommonUtils.getUserSettingsDir(), "active.mojito");
    
    public ActiveDHTNodeController(Vendor vendor, Version version, 
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher) {
        super(vendor, version, dispatcher, DHTMode.ACTIVE);
    }
    
    @Override
    protected MojitoDHT createMojitoDHT(Vendor vendor, Version version) {
        MojitoDHT dht = MojitoFactory.createDHT("ActiveMojitoDHT", vendor, version);
        
        if (DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.getValue() 
                && FILE.exists() && FILE.isFile()) {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(
                        new BufferedInputStream(
                            new SecureInputStream(
                                new FileInputStream(FILE))));
                
                int routeTableVersion = in.readInt();
                if (routeTableVersion >= getRouteTableVersion()) {
                    RouteTable routeTable = (RouteTable)in.readObject();
                    
                    // Do not set the RouteTable here!!! Make sure the Database
                    // base is not corrupt!!!
                    
                    Database database = null;
                    if (DHTSettings.PERSIST_DHT_DATABASE.getValue()) {
                        database = (Database)in.readObject();
                    }
                    
                    // The Database depends on the RouteTable. So if the RouteTable 
                    // is null then there's no point in setting the Database! 
                    if (routeTable != null) {
                        dht.setRouteTable(routeTable);
                        if (database != null) {
                            dht.setDatabase(database);
                        }
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                IOUtils.close(in);
            }
        }
        return dht;
    }

    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }
        
        super.stop();
        
        //Notify our ultrapeers that we disconnected
        sendUpdatedCapabilities();
        
        if (DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.getValue()) {
            ObjectOutputStream out = null;
            try {
                out = new ObjectOutputStream(
                        new BufferedOutputStream(
                            new SecureOutputStream(
                                new FileOutputStream(FILE))));
                
                out.writeInt(getRouteTableVersion());
                synchronized (dht) {
                    out.writeObject(dht.getRouteTable());
                    
                    Database database = null;
                    if (DHTSettings.PERSIST_DHT_DATABASE.getValue()) {
                        database = dht.getDatabase();
                    }
                    out.writeObject(database);
                }
                out.flush();
            } catch (IOException ignored) {
            } finally {
                IOUtils.close(out);
            }
        }
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
        if(!isRunning() || !dht.isBootstrapped()) {
            return Collections.emptyList();
        }
        //return our MRS nodes. We should be first in the list
        return getMRSNodes(maxNodes, false);
    }
}
