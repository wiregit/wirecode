package com.limegroup.gnutella.dht.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.LifecycleEvent;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.statistics.DHTStatsManager;

class ActiveDHTNodeController extends AbstractDHTController {
    
    /**
     * The file to persist this Mojito DHT
     */
    private static final File FILE = new File(CommonUtils.getUserSettingsDir(), "mojito.dat");

    @Override
    public void init() {
        
        DHTStatsManager.clear();
        
        MojitoDHT mojitoDHT = null;
        if (DHTSettings.PERSIST_DHT.getValue() && 
                FILE.exists() && FILE.isFile()) {
            try {
                FileInputStream in = new FileInputStream(FILE);
                mojitoDHT = MojitoFactory.load(in);
                in.close();
            } catch (FileNotFoundException e) {
                LOG.error("FileNotFoundException", e);
            } catch (ClassNotFoundException e) {
                LOG.error("ClassNotFoundException", e);
            } catch (IOException e) {
                LOG.error("IOException", e);
            }
        }
        
        if (mojitoDHT == null) {
            super.dht = MojitoFactory.createDHT("ActiveMojitoDHT");
        } else {
            super.dht = mojitoDHT;
        }
        
        setLimeMessageDispatcher();
    }

    @Override
    public synchronized void start() {
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
    public synchronized void stop() {
        super.stop();
        
        //Notify our ultrapeers that we disconnected
        sendUpdatedCapabilities();
        
        try {
            if(DHTSettings.PERSIST_DHT.getValue()) {
                FileOutputStream out = new FileOutputStream(FILE);
                dht.store(out);
                out.close();
            }
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    }
    
    @Override
    public boolean isActiveNode() {
        return true;
    }

    @Override
    public void handleConnectionLifecycleEvent(LifecycleEvent evt) {
        //handle connection specific events
        Connection c = evt.getConnection();
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
                    //this should never happen if ultrapeers are excluded 
                    //(i.e. they are never active) because as a leaf, we only receive
                    //capabilities VMs from our ultrapeer.
                    LOG.error("Got an ACTIVE CAPABLE VM");
                    throw new IllegalStateException("Active DHT node got an impossible capabilities VM");
                } else {
                    addActiveDHTNode(new InetSocketAddress(host, port));
                }
            } 
        }
        
    }

    @Override
    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        if(!isRunning() || !dht.isBootstrapped()) {
            return Collections.emptyList();
        }
        //return our MRS nodes. We should be first in the list
        return getMRSNodes(maxNodes, false);
    }
}