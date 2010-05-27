package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.LimeWireIOTestModule;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
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
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.BootstrapWorker.BootstrapListener;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;

public class ActiveControllerTest extends DHTTestCase {
    
    private Injector injector;

    public ActiveControllerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ActiveControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws IOException {
        ActiveController.ACTIVE_FILE.delete();
        
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
            }
        });
    }
    
    public void testIsActiveMode() throws IOException {
        ActiveController controller 
            = injector.getInstance(ActiveController.class);
        try {
            assertEquals(DHTMode.ACTIVE, controller.getMode());
            assertTrue(controller.isMode(DHTMode.ACTIVE));
            assertFalse(controller.isRunning());
            assertFalse(controller.isReady());
        } finally {
            controller.close();
        }
    }
    
    public void testPersistence() 
            throws IllegalAccessException, NoSuchFieldException, IOException {
        
        assertFalse(ActiveController.ACTIVE_FILE.exists());
        
        Contact localhost = null;
        Contact[] contacts = null;
        
        ActiveController controller 
            = injector.getInstance(ActiveController.class);
        try {
            MojitoDHT dht = controller.getMojitoDHT();
            localhost = dht.getLocalNode();
            
            RouteTable routeTable = dht.getRouteTable();
            
            for (int i = 0; i < 10; i++) {
                KUID contactId = KUID.createRandomID();
                RemoteContact contact = new RemoteContact(
                        new InetSocketAddress("localhost",4010),
                        ContextSettings.getVendor(),
                        ContextSettings.getVersion(),
                        contactId,
                        new InetSocketAddress("localhost",4010),
                        0,
                        Contact.DEFAULT_FLAG,
                        State.UNKNOWN);
                routeTable.add(contact);
            }
            
            contacts = routeTable.getContacts().toArray(new Contact[0]);
            
        } finally {
            controller.close();
        }
        
        assertTrue(ActiveController.ACTIVE_FILE.exists());
        
        controller = injector.getInstance(ActiveController.class);
        try {
            MojitoDHT dht = controller.getMojitoDHT();
            assertEquals(localhost.getNodeID(), dht.getLocalNodeID());
            
            Contact[] others = (Contact[])PrivilegedAccessor.getValue(
                    controller, "contacts");
            assertNotNull(others);
            assertEquals(contacts.length-1, others.length);
            
        } finally {
            controller.close();
        }
    }
    
    public void testBootstrap() throws IOException, InterruptedException {
        MojitoDHT dhtBootstrapNode 
            = startBootstrapDHT(injector.getInstance(
                    LifecycleManager.class));
        try {
            
            ActiveController controller 
                = injector.getInstance(ActiveController.class);
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
                    public void handleReady() {
                        latch.countDown();
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
}
