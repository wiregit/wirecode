package com.limegroup.gnutella.dht2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.IOUtils;
import org.limewire.io.SecureInputStream;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.storage.Database;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.connection.Connection;
import com.limegroup.gnutella.connection.ConnectionCapabilities;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;

class ActiveController extends Controller {
    
    private static final Log LOG 
        = LogFactory.getLog(ActiveController.class);
    
    private static final File ACTIVE_FILE 
        = new File(CommonUtils.getUserSettingsDir(), "active.mojito");
    
    public ActiveController() {
        super(DHTMode.ACTIVE);
    }

    @Override
    public boolean isRunning() {
        return false;
    }
    
    @Override
    public void start() {
    }

    @Override
    public void close() throws IOException {
    }
    
    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        
        // Ignore everything that's not a Connection Event
        Connection connection = evt.getConnection();
        if (connection == null) {
            return;
        }
        
        if (!evt.isConnectionCapabilitiesEvent()) {
            return;
        }
        
        ConnectionCapabilities capabilities 
            = connection.getConnectionCapabilities();
        String host = connection.getAddress();
        int port = connection.getPort();

        if (capabilities.remostHostIsPassiveDHTNode() > -1) {
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Connection is passive dht node: "+ connection);
            }
            addPassiveDHTNode(new InetSocketAddress(host, port));
            
        } else if(capabilities.remostHostIsActiveDHTNode() > -1) {
            
            if (DHTSettings.EXCLUDE_ULTRAPEERS.getValue()) {
                return;
            } 
            addActiveDHTNode(new InetSocketAddress(host, port));
        } 
    }
    
    private static Handle read() {
        if (DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.getValue() 
                && ACTIVE_FILE.exists() && ACTIVE_FILE.isFile()) {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(
                        new BufferedInputStream(
                            new SecureInputStream(
                                new FileInputStream(ACTIVE_FILE))));
                
                // Pre-condition: The Database depends on the RouteTable
                // If there's no persisted RouteTable or it's corrupt
                // then there's no point in reading or setting the 
                // Database!
                
                int routeTableVersion = in.readInt();
                if (routeTableVersion >= DHTSettings.ACTIVE_DHT_ROUTETABLE_VERSION.getValue()) {
                    RouteTable routeTable = (RouteTable)in.readObject();

                    Database database = null;
                    try {
                        if (DHTSettings.PERSIST_DHT_DATABASE.getValue()) {
                            database = (Database)in.readObject();                                                        
                        }
                    } catch (Throwable ignored) {
                        LOG.error("Throwable", ignored);
                    }
                    
                    // The Database depends on the RouteTable!
                    if (routeTable != null) {
                        long maxElaspedTime 
                            = DHTSettings.MAX_ELAPSED_TIME_SINCE_LAST_CONTACT.getValue();
                        if (maxElaspedTime < Long.MAX_VALUE) {
                            routeTable.purge(maxElaspedTime);
                        }
                    }
                    
                    return new Handle(routeTable, database);
                }
            } catch (Throwable ignored) {
                LOG.error("Throwable", ignored);
            } finally {
                IOUtils.close(in);
            }
        }
        
        return null;
    }
    
    private static class Handle {
        
        private final RouteTable routeTable;
        
        private final Database database;
        
        public Handle(RouteTable routeTable, Database database) {
            this.routeTable = routeTable;
            this.database = database;
        }
    }
}
