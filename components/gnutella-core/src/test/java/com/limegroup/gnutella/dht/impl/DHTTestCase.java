package com.limegroup.gnutella.dht.impl;

import java.net.InetSocketAddress;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.PingPongSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.MojitoDHT;

public abstract class DHTTestCase extends BaseTestCase {
    
    protected static final int PORT = 6667;
    
    protected static RouterService ROUTER_SERVICE;
    
    protected static MojitoDHT BOOTSTRAP_DHT;
    
    protected static final int BOOTSTRAP_DHT_PORT = 3000;

    public DHTTestCase(String name) {
        super(name);
    }
    
    public static void globalSetUp() throws Exception {
        //setup bootstrap node
        BOOTSTRAP_DHT = new MojitoDHT("bootstrapNode");
        InetSocketAddress addr = new InetSocketAddress("localhost", BOOTSTRAP_DHT_PORT);
        BOOTSTRAP_DHT.bind(addr);
        BOOTSTRAP_DHT.start();
        
        ROUTER_SERVICE = new RouterService(new ActivityCallbackStub());
    }
    
    protected void setSettings() {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                new String[] {"127.*.*.*", "18.239.0.*"});
        ConnectionSettings.PORT.setValue(PORT);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        assertEquals("unexpected port", PORT, 
                ConnectionSettings.PORT.getValue());
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);    
        ConnectionSettings.USE_GWEBCACHE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        PingPongSettings.PINGS_ACTIVE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        //dht settings:
        DHTSettings.PERSIST_DHT.setValue(false);
    }
    
    public static void globalTearDown() throws Exception {
        BOOTSTRAP_DHT.stop();
    }

}
