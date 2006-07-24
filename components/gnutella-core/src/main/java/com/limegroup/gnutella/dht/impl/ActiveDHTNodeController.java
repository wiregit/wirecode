package com.limegroup.gnutella.dht.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;

class ActiveDHTNodeController extends AbstractDHTController {
    
    private static final Log LOG = LogFactory.getLog(ActiveDHTNodeController.class);
    
    /**
     * The file to persist this Mojito DHT
     */
    private static final File FILE = new File(CommonUtils.getUserSettingsDir(), "mojito.dat");

    public void init() {
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
        //if we want to connect actively, we either shouldn't be an ultrapeer
        //or should be DHT capable
        if (!DHTSettings.FORCE_DHT_CONNECT.getValue() 
                && !DHTSettings.ACTIVE_DHT_CAPABLE.getValue()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Cannot initialize DHT - node is not DHT capable or is an ultrapeer");
            }
            return;
        }
        
        super.start();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        
        //Notify our connections that we disconnected
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
    protected void sendUpdatedCapabilities() {
        CapabilitiesVM.reconstructInstance();
        RouterService.getConnectionManager().sendUpdatedCapabilities();
    }

    public boolean isActiveNode() {
        return true;
    }

    @Override
    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        if(!isRunning() || isWaiting()) {
            return Collections.emptyList();
        }
        
        //IF we are active and bootstrapped: we need only return ourselves
        IpPort localNode = new IpPortContactNode(((Context)dht).getLocalNode());
        return Arrays.asList(localNode);
    }
}