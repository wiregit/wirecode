package com.limegroup.gnutella.dht2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.DHTValueFuture;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;

class PassiveController extends AbstractController {

    private static final Log LOG 
        = LogFactory.getLog(PassiveController.class);
    
    private static final File PASSIVE_FILE 
        = new File(CommonUtils.getUserSettingsDir(), "passive.mojito");
    
    public PassiveController() {
        super(DHTMode.PASSIVE);
    }
    
    @Override
    public MojitoDHT getMojitoDHT() {
        return null;
    }

    @Override
    public boolean isRunning() {
        return false;
    }
    
    @Override
    public boolean isReady() {
        return false;
    }
    
    @Override
    public void start() {
    }

    @Override
    public void close() throws IOException {
    }
    
    @Override
    public DHTFuture<ValueEntity> get(EntityKey key) {
        return new DHTValueFuture<ValueEntity>(new UnsupportedOperationException());
    }

    @Override
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value) {
        return new DHTValueFuture<StoreEntity>(new UnsupportedOperationException());
    }
    
    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        /*// Ignore everything that's not a Connection Event
        Connection connection = evt.getConnection();
        if (connection == null) {
            return;
        }
        
        String host = connection.getAddress();
        int port = connection.getPort();
        
        if (evt.isConnectionClosedEvent()) {
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Got a connection closed event for connection: "+ connection);
            }
            
            removeLeafDHTNode( host , port );
            
        } else if (evt.isConnectionCapabilitiesEvent()){
            
            ConnectionCapabilities capabilities 
                = connection.getConnectionCapabilities();
            
            if (capabilities.remostHostIsActiveDHTNode() > -1) {
                
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Connection is active dht node: "+ connection);
                }
                addLeafDHTNode( host , port );
                
            } else if(capabilities.remostHostIsPassiveDHTNode() > -1) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Connection is passive dht node: "+ connection);
                }
                addPassiveDHTNode(new InetSocketAddress(host, port));
                
            } else {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Connection is node not connected to the DHT network: "+ connection);
                }
                removeLeafDHTNode( host , port );
            }
        } 
        */
    }
}
