package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.PingPongSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.MojitoFactory;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.Contact.State;
import com.limegroup.mojito.routing.impl.RemoteContact;
import com.limegroup.mojito.settings.ContextSettings;

public abstract class DHTTestCase extends BaseTestCase {
    
    protected static final int PORT = 6667;
    
    protected static RouterService ROUTER_SERVICE;
    
    protected static MojitoDHT BOOTSTRAP_DHT;
    
    protected static final int BOOTSTRAP_DHT_PORT = 3000;
    
    protected static List<MojitoDHT> DHT_LIST = new ArrayList<MojitoDHT>();

    public DHTTestCase(String name) {
        super(name);
    }
    
    public static void globalSetUp() throws Exception {
        //setup bootstrap node
        BOOTSTRAP_DHT = MojitoFactory.createDHT("bootstrapNode");
        InetSocketAddress addr = new InetSocketAddress(BOOTSTRAP_DHT_PORT);
        BOOTSTRAP_DHT.bind(addr);
        BOOTSTRAP_DHT.start();
        DHT_LIST.add(BOOTSTRAP_DHT);
        
        ROUTER_SERVICE = new RouterService(new ActivityCallbackStub());
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ROUTER_SERVICE.start();
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
        for(MojitoDHT dht : DHT_LIST) {
            dht.stop();
        }
        RouterService.shutdown();
    }

    protected void fillRoutingTable(RouteTable rt, int numNodes) {
        for(int i = 0; i < numNodes; i++) {
            KUID kuid = KUID.createRandomID();
            RemoteContact node = new RemoteContact(
                    new InetSocketAddress("localhost",4000+i),
                    ContextSettings.VENDOR.getValue(),
                    ContextSettings.VERSION.getValue(),
                    kuid,
                    new InetSocketAddress("localhost",4000+i),
                    0,
                    Contact.DEFAULT_FLAG,
                    State.UNKNOWN);
            rt.add(node);
        }
    }
}