package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.LimeWireIOTestModule;
import org.limewire.mojito.MojitoUtils;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.DatagramTransport;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.io.ValueResponseHandler.NoSuchValueException;
import org.limewire.mojito2.message.DefaultMessageFactory;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.routing.RouteTable.RouteTableEvent;
import org.limewire.mojito2.routing.RouteTable.RouteTableListener;
import org.limewire.mojito2.settings.KademliaSettings;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueImpl;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.mojito2.util.ExceptionUtils;
import org.limewire.mojito2.util.HostFilter;
import org.limewire.mojito2.util.IoUtils;
import org.limewire.util.StringUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.dht2.DHTManager;
import com.limegroup.gnutella.dht2.PassiveLeafController;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;

public class PassiveLeafControllerTest extends DHTTestCase {

    private Injector injector;
    
    public PassiveLeafControllerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PassiveLeafControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        DHTTestUtils.setSettings(PORT);
        
        final Transport transport 
            = new DatagramTransport(5000);
        
        injector = LimeTestUtils.createInjectorNonEagerly(
                new LimeWireIOTestModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(DHTManager.class).to(DHTManagerStub.class);
                bind(NetworkManager.class).to(NetworkManagerStub.class);
                bind(HostFilter.class).to(HostFilterStub.class);
                bind(Transport.class).toInstance(transport);
                bind(MessageFactory.class).to(DefaultMessageFactory.class);
                bind(NodeAssigner.class).to(NodeAssignerStub.class);
            }
        });
    }
    
    public void testIsPassiveLeafMode() throws IOException {
        PassiveLeafController controller 
            = injector.getInstance(PassiveLeafController.class);
        try {
            assertEquals(DHTMode.PASSIVE_LEAF, controller.getMode());
            assertTrue(controller.isMode(DHTMode.PASSIVE_LEAF));
            assertFalse(controller.isRunning());
            assertFalse(controller.isReady());
            
            MojitoDHT dht = controller.getMojitoDHT();
            Contact localhost = dht.getLocalNode();
            assertTrue(localhost.isFirewalled());
            
            controller.start();
            
            assertTrue(controller.isRunning());
            assertTrue(controller.isReady());
            
        } finally {
            controller.close();
        }
    }
    
    public void testLookup() throws IOException, InterruptedException, 
            ExecutionException {
        
        DHTTestUtils.setLocalIsPrivate(injector, false);
        
        final int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        PassiveLeafController controller 
            = injector.getInstance(PassiveLeafController.class);
        try {
            
            controller.start();
            
            List<MojitoDHT> dhts = MojitoUtils.createBootStrappedDHTs(3, 5001);
            try {
                // Store a DHTValue
                KUID key = KUID.createRandomID();
                DHTValue value = new DHTValueImpl(
                        DHTValueType.BINARY, 
                        Version.ZERO, 
                        StringUtils.toAsciiBytes("Hello World"));
                
                StoreEntity result = dhts.get(0).put(key, value).get();
                assertEquals(k, result.getContacts().length);
                
                EntityKey lookupKey = EntityKey.createEntityKey(
                        key, DHTValueType.ANY);
                
                // Try to get the value which should fail
                try {
                    controller.get(lookupKey).get();
                    fail("Should have failed!");
                } catch (ExecutionException err) {
                    if (!ExceptionUtils.isCausedBy(err, 
                            NoSuchValueException.class)) {
                        fail(err);
                    }
                }
                
                // Ping a Node which will add it to the passive leafs RouteTable
                MojitoDHT dht = controller.getMojitoDHT();
                dht.setContactAddress(new InetSocketAddress("localhost", 5000));
                RouteTable routeTable = dht.getRouteTable();
                
                final CountDownLatch latch = new CountDownLatch(1);
                routeTable.addRouteTableListener(new RouteTableListener() {
                    @Override
                    public void handleRouteTableEvent(RouteTableEvent event) {
                        switch (event.getEventType()) {
                            case ADD_ACTIVE_CONTACT:
                            case ADD_CACHED_CONTACT:
                            case UPDATE_CONTACT:
                                latch.countDown();
                                break;
                        }
                    }
                });
                
                // The RouteTable contains the localhost only
                assertEquals(1, routeTable.size());
                dht.ping(dhts.get(0).getContactAddress()).get();
                
                // The RouteTable should contain the host we've just PINGed
                if (!latch.await(1, TimeUnit.SECONDS)) {
                    fail("Shouldn't have failed!");
                }
                assertEquals(2, routeTable.size());
                
                // Try again and it should work now
                try {
                    ValueEntity r = controller.get(lookupKey).get();
                    if (r.getEntities().length == 0) {
                        fail("Should have found DHTValue");
                    }
                } catch (ExecutionException err) {
                    fail(err);
                }
                
            } finally {
                IoUtils.closeAll(dhts);
            }
            
        } finally {
            controller.close();
        }
    }
}
