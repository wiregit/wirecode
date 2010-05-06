package com.limegroup.gnutella.dht2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;

class PassiveController extends Controller {

    private static final Log LOG 
        = LogFactory.getLog(PassiveController.class);
    
    private static final File PASSIVE_FILE 
        = new File(CommonUtils.getUserSettingsDir(), "passive.mojito");
    
    public PassiveController() {
        super(DHTMode.PASSIVE);
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
