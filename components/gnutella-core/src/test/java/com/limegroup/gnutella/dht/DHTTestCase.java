package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Contact.State;
import org.limewire.mojito.routing.impl.RemoteContact;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.NetworkSettings;

import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.PingPongSettings;
import com.limegroup.gnutella.util.LimeTestCase;

public abstract class DHTTestCase extends LimeTestCase {
    
    protected static final int BOOTSTRAP_DHT_PORT = 3000;
    
    protected static final int PORT = 6667;
    
    private boolean bootstrapped = false;
    
    public DHTTestCase(String name) {
        super(name);
    }
    
    protected MojitoDHT startBootstrapDHT(LifecycleManager lifeCycleManager) throws Exception {
        assertFalse("bootstrap DHT already started", bootstrapped);
        bootstrapped = true;
        
        // setup bootstrap node
        MojitoDHT bootstrapDHT = MojitoFactory.createDHT("bootstrapNode");
        bootstrapDHT.bind(new InetSocketAddress(BOOTSTRAP_DHT_PORT));
        bootstrapDHT.start();
        
        ConnectionSettings.PORT.setValue(PORT);
        ConnectionSettings.FORCED_PORT.setValue(PORT);
        
        assertEquals("unexpected port", PORT, ConnectionSettings.PORT.getValue());
        
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        lifeCycleManager.start();
        
        return bootstrapDHT;
    }
    
    protected void setSettings() {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                new String[] {"127.*.*.*", "18.239.0.*"});
                
        ConnectionSettings.PORT.setValue(PORT);
        assertEquals("unexpected port", PORT, ConnectionSettings.PORT.getValue());
                
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        PingPongSettings.PINGS_ACTIVE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        // DHT Settings
        DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.setValue(false);
        DHTSettings.PERSIST_DHT_DATABASE.setValue(false);
        ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.setValue(0);
        
        NetworkSettings.FILTER_CLASS_C.setValue(false);
        NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        // We're working on the loopback. Everything should be done
        // in less than 500ms
        NetworkSettings.DEFAULT_TIMEOUT.setValue(500);
        
        // Nothing should take longer than 1.5 seconds. If we start seeing
        // LockTimeoutExceptions on the loopback then check this Setting!
        ContextSettings.WAIT_ON_LOCK.setValue(1500);
    }
    
//    public static void globalTearDown() throws Exception {
//        if (startDHT) {
//            close(DHT_LIST);
//        }
//        
//        DHT_LIST.clear();
//        BOOTSTRAP_DHT = null;
//    }

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
    
    protected static void close(Collection<? extends MojitoDHT> dhts) {
        for (MojitoDHT dht : dhts) {
            dht.close();
        }
    }
}
