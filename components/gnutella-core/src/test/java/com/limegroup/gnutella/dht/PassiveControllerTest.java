package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.LimeWireIOTestModule;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoUtils;
import org.limewire.mojito.entity.CollisionException;
import org.limewire.mojito.io.DatagramTransport;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.message.DefaultMessageFactory;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RemoteContact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Contact.State;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.util.HostFilter;
import org.limewire.mojito.util.IoUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;

public class PassiveControllerTest extends DHTTestCase {
    
    private Injector injector;

    public PassiveControllerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PassiveControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws IOException {
        PassiveController.PASSIVE_FILE.delete();
        
        final Transport transport 
            = new DatagramTransport(5000);
        
        injector = LimeTestUtils.createInjectorNonEagerly(
                new LimeWireIOTestModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(DHTManager.class).to(DHTManagerStub.class);
                bind(NetworkManager.class).to(NetworkManagerStub.class);
                bind(HostFilter.class).toInstance(new HostFilterStub());
                bind(Transport.class).toInstance(transport);
                bind(MessageFactory.class).to(DefaultMessageFactory.class);
                bind(NodeAssigner.class).to(NodeAssignerStub.class);
            }
        });
    }
    
    public void testIsPassiveMode() throws IOException {
        PassiveController controller 
            = injector.getInstance(PassiveController.class);
        try {
            assertEquals(DHTMode.PASSIVE, controller.getMode());
            assertTrue(controller.isMode(DHTMode.PASSIVE));
            assertFalse(controller.isRunning());
            assertFalse(controller.isReady());
            
            MojitoDHT dht = controller.getMojitoDHT();
            Contact localhost = dht.getLocalhost();
            assertTrue(localhost.isFirewalled());
            
        } finally {
            controller.close();
        }
    }
    
    public void testPersistence() throws IllegalAccessException, NoSuchFieldException, IOException {

        assertFalse(PassiveController.PASSIVE_FILE.exists());
        
        Contact[] contacts = null;

        PassiveController controller 
            = injector.getInstance(PassiveController.class);
        try {

            MojitoDHT dht = controller.getMojitoDHT();
            RouteTable routeTable = dht.getRouteTable();

            for (int i = 0; i < 10; i++) {
                KUID contactId = KUID.createRandomID();
                RemoteContact contact = new RemoteContact(
                        new InetSocketAddress("localhost", 4010),
                        ContextSettings.getVendor(), 
                        ContextSettings.getVersion(), 
                        contactId,
                        new InetSocketAddress("localhost", 4010), 
                        0, Contact.DEFAULT_FLAG,
                        State.UNKNOWN);
                routeTable.add(contact);
            }

            contacts = routeTable.getContacts().toArray(new Contact[0]);

        } finally {
            controller.close();
        }

        assertTrue(PassiveController.PASSIVE_FILE.exists());

        controller = injector.getInstance(PassiveController.class);
        try {
            Contact[] others = (Contact[]) PrivilegedAccessor.getValue(controller, "contacts");
            assertNotNull(others);
            assertEquals(contacts.length - 1, others.length);
        } finally {
            controller.close();
        }
    }
    
    public void testBootstrap() throws IOException, InterruptedException {
        MojitoDHT dhtBootstrapNode 
            = startBootstrapDHT(injector.getInstance(
                    LifecycleManager.class));
        try {
            
            PassiveController controller 
                = injector.getInstance(PassiveController.class);
            try {
                assertFalse(controller.isRunning());
                assertFalse(controller.isReady());
                
                controller.start();
                assertTrue(controller.isRunning());
                assertFalse(controller.isReady());
                
                MojitoDHT dht = controller.getMojitoDHT();
                dht.setContactAddress(new InetSocketAddress("localhost", 5000));
                
                BootstrapWorker worker 
                    = controller.getBootstrapWorker();
                
                final CountDownLatch latch = new CountDownLatch(1);
                worker.addBootstrapListener(new BootstrapListener() {
                    @Override
                    public void handleConnected(boolean success) {
                        if (success) {
                            latch.countDown();
                        }
                    }
                    
                    @Override
                    public void handleConnecting() {
                    }

                    @Override
                    public void handleCollision(CollisionException ex) {
                        fail("Collision!");
                    }
                });
                
                // We pretend we received a SocketAddress through Gnutella.
                // It should bootstrap the ActiveController's DHT node!
                SocketAddress address = dhtBootstrapNode.getContactAddress();
                controller.addActiveNode(address);
                
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    fail("Failed to bootstrap!");
                }
                
                assertTrue(controller.isReady());
                
            } finally {
                controller.close();
            }
        } finally {
            dhtBootstrapNode.close();
        }
    }
    
    public void testAddingRemovingLeafNodes() throws IOException, 
            InterruptedException, ExecutionException {
        DHTTestUtils.setLocalIsPrivate(injector, false);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        
        PassiveController controller 
            = injector.getInstance(PassiveController.class);
        try {
            
            MojitoDHT localhost = controller.getMojitoDHT();
            localhost.setContactAddress(new InetSocketAddress("localhost", 5000));
            
            controller.start();
            
            PassiveRouteTable routeTable 
                = controller.getPassiveRouteTable();
            
            List<MojitoDHT> dhts = MojitoUtils.createBootStrappedDHTs(1, 5001);
            try {
                for (MojitoDHT dht : dhts) {
                    controller.addLeafNode(dht.getContactAddress());
                }
                
                // Wait a bit for the PONGs
                Thread.sleep(1000);
                
            } finally {
                IoUtils.closeAll(dhts);
            }
            
            assertEquals(routeTable.getDHTLeaves().size(), 
                    routeTable.getActiveContacts().size()-1);
            
            // Remove 2-Nodes
            assertNotNull(routeTable.removeLeafNode(
                    new InetSocketAddress("localhost", 5001)));
            assertNotNull(routeTable.removeLeafNode(
                    new InetSocketAddress("localhost", 5002)));
            
            // Make sure they're no longer there.
            assertFalse(routeTable.getDHTLeaves().contains(
                    new InetSocketAddress("localhost", 5001)));
            assertFalse(routeTable.getDHTLeaves().contains(
                    new InetSocketAddress("localhost", 5002)));
            
        } finally {
            controller.close();
        }
    }
}
