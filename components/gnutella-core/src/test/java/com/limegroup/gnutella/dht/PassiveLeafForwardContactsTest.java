package com.limegroup.gnutella.dht;

import java.io.InterruptedIOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.Test;

import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.handshaking.LeafHeaders;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.PingPongSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.LimeTestCase;

/*
 * DHT <----> Ultrapeer <----> Leaf
 * 
 * Everytime an Ultrapeer learns of a new Contact or updates an
 * existing Contact it will forward it to its leafs
 */
public class PassiveLeafForwardContactsTest extends LimeTestCase {
    
    private static final int PORT = 6667;
    
    private List<MojitoDHT> dhts;
    
    private int k;
    
    public PassiveLeafForwardContactsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PassiveLeafForwardContactsTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.MAX_LEAVES.setValue(33);
        ConnectionSettings.NUM_CONNECTIONS.setValue(33);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);    
        ConnectionSettings.USE_GWEBCACHE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
        
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.USE_GWEBCACHE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        PingPongSettings.PINGS_ACTIVE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                new String[] {"127.*.*.*"});
        
        ConnectionSettings.PORT.setValue(PORT);
        ConnectionSettings.FORCED_PORT.setValue(PORT);
        
        assertEquals("unexpected port", PORT, 
                 ConnectionSettings.PORT.getValue());
        
        DHTSettings.DISABLE_DHT_USER.setValue(false);
        DHTSettings.DISABLE_DHT_NETWORK.setValue(false);
        DHTSettings.ENABLE_PASSIVE_LEAF_MODE.setValue(true);
        DHTSettings.PERSIST_DHT.setValue(false);
        KademliaSettings.SHUTDOWN_MULTIPLIER.setValue(0);
        NetworkSettings.TIMEOUT.setValue(500);
        NetworkSettings.BOOTSTRAP_TIMEOUT.setValue(500);
        NetworkSettings.STORE_TIMEOUT.setValue(500);
        
        if (!RouterService.isLoaded()) {
            // Start an instance of LimeWire in Ultrapeer mode
            RouterService routerService 
                = new RouterService(new ActivityCallbackStub());
            routerService.start();
            
            RouterService.clearHostCatcher();
            RouterService.connect();
            
            // Make sure LimeWire is running as an Ultrapeer
            assertTrue(RouterService.isStarted());
            assertTrue(RouterService.isSupernode());
            assertFalse(RouterService.isActiveSuperNode());
            assertFalse(RouterService.isShieldedLeaf());
        }
        
        // Start and bootstrap a bunch of DHT Nodes
        k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        dhts = new ArrayList<MojitoDHT>();
        for (int i = 0; i < 2*k; i++) {
            MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
            dht.bind(2000 + i);
            dht.start();
            
            if (i > 0) {
                dht.bootstrap(dhts.get(i-1).getContactAddress()).get();
            }
            
            dhts.add(dht);
        }
        dhts.get(0).bootstrap(dhts.get(1).getContactAddress()).get();
    }

    @Override
    protected void tearDown() throws Exception {
        for (MojitoDHT dht : dhts) {
            dht.close();
        }
        dhts = null;
    }
    
    public static void globalTearDown() throws Exception {
        RouterService.shutdown();
    }
    
    public void testForwardContacts() throws Exception {
        // There should be no connections
        ConnectionManager cm = RouterService.getConnectionManager();
        assertEquals(0, cm.getNumConnections());

        // Connect a leaf Node to the Ultrapeer
        Connection out = createLeafConnection();
        
        try {
            assertTrue(out.isOpen());
            assertTrue(out.isOutgoing());
            drain(out);
            
            // There should be one connection now
            assertEquals(1, cm.getNumConnections());
            assertTrue(RouterService.isActiveSuperNode());
            
            // Check a few more things
            ManagedConnection in = (ManagedConnection)cm.getConnections().get(0);
            assertFalse(in.isOutgoing());
            assertTrue(in.isSupernodeClientConnection());
            assertEquals(-1, in.remoteHostIsPassiveLeafNode());
            assertFalse(in.isPushProxyFor());
            
            // Pretend the leaf is running in PASSIVE_LEAF mode.
            // Thanks to Singletons trivial to do... ;)
            Method m = CapabilitiesVM.class.getDeclaredMethod("getSupportedMessages", new Class[]{});
            m.setAccessible(true);
            Set capabilites = (Set)m.invoke(null, new Object[0]);
            
            Class clazz = Class.forName("com.limegroup.gnutella.messages.vendor.CapabilitiesVM$SupportedMessageBlock");
            Object smb = PrivilegedAccessor.invokeConstructor(clazz, 
                    new Object[]{ DHTMode.PASSIVE_LEAF.getCapabilityName(), Integer.valueOf(0) }, 
                    new Class[] { byte[].class, int.class });
            capabilites.add(smb);
            
            CapabilitiesVM vm = (CapabilitiesVM)PrivilegedAccessor.invokeConstructor(
                    CapabilitiesVM.class, new Object[] { capabilites }, new Class[] { Set.class });
            assertEquals(0, vm.isPassiveLeafNode());
            
            // Tell our Ultrapeer that we've PASSIVE_LEAF mode enabled
            out.send(vm);
            out.flush();
            
            // A second requirement is that the given Ultrapeer is our
            // Push Proxy
            out.send(new PushProxyRequest(new GUID(GUID.makeGuid())));
            out.flush();
            
            Thread.sleep(250);
            
            // And did it work?
            assertNotEquals(-1, in.remoteHostIsPassiveLeafNode());
            assertTrue(in.isPushProxyFor());
            
            RouterService.getDHTManager().start(DHTMode.PASSIVE);
            Thread.sleep(250);
            assertEquals(DHTMode.PASSIVE, RouterService.getDHTMode());
            
            
            // Bootstrap the Ultrapeer
            RouterService.getDHTManager().getMojitoDHT()
                .bootstrap(dhts.get(0).getContactAddress()).get();
            
            // -------------------------------------
            
            // And check what the leaf is receiving...
            int count = 0;
            Set<Contact> nodes = new LinkedHashSet<Contact>();
            try {
                while(true) {
                    Message msg = out.receive(2000);
                    if (msg instanceof DHTContactsMessage) {
                        DHTContactsMessage message = (DHTContactsMessage)msg;
                        nodes.addAll(message.getContacts());
                        count += message.getContacts().size();
                    }
                }
            } catch (InterruptedIOException err) {
                if (nodes.isEmpty()) {
                    fail(err);
                }
            }
            
            assertGreaterThanOrEquals(nodes.size(), count);
            
            // Note: This assert can sometimes fail! It's not a bug!
            // The reason for this lies in the lookup algorithm which
            // terminates as soon as it can't find any closer Nodes.
            // That means our Ultrapeer may not hit all 2*k DHT Nodes
            // and will therefore send us less. 
            //assertEquals(2*k, nodes.size());
            
            // See above
            assertTrue("k=" + k + ", size=" + nodes.size(), 
                    nodes.size() >= k && nodes.size() <= 2*k);
        } finally {
            out.close();
        }
    }
    
    public void testDoNotSendCapabilitiesVM() throws Exception {
        // There should be no connections
        ConnectionManager cm = RouterService.getConnectionManager();
        assertEquals(0, cm.getNumConnections());

        // Connect a leaf Node to the Ultrapeer
        Connection out = createLeafConnection();
        try {
            assertTrue(out.isOpen());
            assertTrue(out.isOutgoing());
            drain(out);
            
            // There should be one connection now
            assertEquals(1, cm.getNumConnections());
            assertTrue(RouterService.isActiveSuperNode());
            
            // Check a few more things
            ManagedConnection in = (ManagedConnection)cm.getConnections().get(0);
            assertFalse(in.isOutgoing());
            assertTrue(in.isSupernodeClientConnection());
            assertEquals(-1, in.remoteHostIsPassiveLeafNode());
            assertFalse(in.isPushProxyFor());
            
            // --- WE DO NOT PRETEND TO BE IN PASSIVE_LEAF MODE !!! ---
            
            // A second requirement is that the given Ultrapeer is our
            // Push Proxy
            out.send(new PushProxyRequest(new GUID(GUID.makeGuid())));
            out.flush();
            
            Thread.sleep(2000);
            
            // And did it work?
            assertEquals(-1, in.remoteHostIsPassiveLeafNode());
            assertTrue(in.isPushProxyFor());
            
            RouterService.getDHTManager().start(DHTMode.PASSIVE);
            Thread.sleep(250);
            assertEquals(DHTMode.PASSIVE, RouterService.getDHTMode());
            
            // Bootstrap the Ultrapeer
            RouterService.getDHTManager().getMojitoDHT()
                .bootstrap(dhts.get(0).getContactAddress()).get();
            
            // -------------------------------------
            
            // And check what the leaf is receiving
            try {
                while(true) {
                    Message msg = out.receive(2000);
                    if (msg instanceof DHTContactsMessage) {
                        fail("Should not have received a DHTContactsMessage: " + msg);
                    }
                }
            } catch (InterruptedIOException expected) {
            }
        } finally {
            out.close();
        }
    }
    
    public void testDoNotSendPushProxyRequest() throws Exception {
        // There should be no connections
        ConnectionManager cm = RouterService.getConnectionManager();
        assertEquals(0, cm.getNumConnections());

        // Connect a leaf Node to the Ultrapeer
        Connection out = createLeafConnection();
        try {
            assertTrue(out.isOpen());
            assertTrue(out.isOutgoing());
            drain(out);
            
            // There should be one connection now
            assertEquals(1, cm.getNumConnections());
            assertTrue(RouterService.isActiveSuperNode());
            
            // Check a few more things
            ManagedConnection in = (ManagedConnection)cm.getConnections().get(0);
            assertFalse(in.isOutgoing());
            assertTrue(in.isSupernodeClientConnection());
            assertEquals(-1, in.remoteHostIsPassiveLeafNode());
            assertFalse(in.isPushProxyFor());
            
            // Pretend the leaf is running in PASSIVE_LEAF mode.
            // Thanks to Singletons trivial to do... ;)
            Method m = CapabilitiesVM.class.getDeclaredMethod("getSupportedMessages", new Class[]{});
            m.setAccessible(true);
            Set capabilites = (Set)m.invoke(null, new Object[0]);
            
            Class clazz = Class.forName("com.limegroup.gnutella.messages.vendor.CapabilitiesVM$SupportedMessageBlock");
            Object smb = PrivilegedAccessor.invokeConstructor(clazz, 
                    new Object[]{ DHTMode.PASSIVE_LEAF.getCapabilityName(), Integer.valueOf(0) }, 
                    new Class[] { byte[].class, int.class });
            capabilites.add(smb);
            
            CapabilitiesVM vm = (CapabilitiesVM)PrivilegedAccessor.invokeConstructor(
                    CapabilitiesVM.class, new Object[] { capabilites }, new Class[] { Set.class });
            assertEquals(0, vm.isPassiveLeafNode());
            
            // Tell our Ultrapeer that we've PASSIVE_LEAF mode enabled
            out.send(vm);
            out.flush();
            
            // --- But don't tell the Ultrapeer its our Push Proxy! ---
            
            Thread.sleep(2000);
            
            // And did it work?
            assertNotEquals(-1, in.remoteHostIsPassiveLeafNode());
            assertFalse(in.isPushProxyFor());
            
            RouterService.getDHTManager().start(DHTMode.PASSIVE);
            Thread.sleep(250);
            assertEquals(DHTMode.PASSIVE, RouterService.getDHTMode());
            
            // Bootstrap the Ultrapeer
            RouterService.getDHTManager().getMojitoDHT()
                .bootstrap(dhts.get(0).getContactAddress()).get();
            
            // -------------------------------------
            
            // And check what the leaf is receiving
            try {
                while(true) {
                    Message msg = out.receive(2000);
                    if (msg instanceof DHTContactsMessage) {
                        fail("Should not have received a DHTContactsMessage: " + msg);
                    }
                }
            } catch (InterruptedIOException expected) {
            }
        } finally {
            out.close();
        }
    }
    
    protected Connection createLeafConnection() throws Exception {
        return createConnection(new LeafHeaders("localhost"));
    }
    
    /** Builds a single connection with the given headers. */
    protected Connection createConnection(Properties headers) throws Exception {
        Connection c = new ManagedConnection("localhost", PORT);
        c.initialize(headers, new EmptyResponder(), 1000);
        return c;
    }
}
