package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Contact.State;
import org.limewire.mojito.routing.impl.RemoteContact;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.NetworkSettings;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.PingPongSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;

public abstract class DHTTestCase extends LimeTestCase {
    
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
        KademliaSettings.SHUTDOWN_MULTIPLIER.setValue(0);
        NetworkSettings.TIMEOUT.setValue(500);
        NetworkSettings.BOOTSTRAP_TIMEOUT.setValue(500);
        NetworkSettings.STORE_TIMEOUT.setValue(500);
    }
    
    public static void globalTearDown() throws Exception {
        for(MojitoDHT dht : DHT_LIST) {
            dht.close();
        }
        RouterService.shutdown();
    }

    protected void fillRoutingTable(RouteTable rt, int numNodes) {
        for(int i = 0; i < numNodes; i++) {
            KUID kuid = KUID.createRandomID();
            RemoteContact node = new RemoteContact(
                    new InetSocketAddress("localhost",4000+i),
                    ContextSettings.getVendor(),
                    ContextSettings.getVersion(),
                    kuid,
                    new InetSocketAddress("localhost",4000+i),
                    0,
                    Contact.DEFAULT_FLAG,
                    State.UNKNOWN);
            rt.add(node);
        }
    }
}
