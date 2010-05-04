package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.mojito.concurrent.DHTExecutorService;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.statistics.DHTStats;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueFactoryManager;
import org.limewire.mojito2.storage.Database;
import org.limewire.mojito2.storage.EvictorManager;
import org.limewire.mojito2.storage.StorableModelManager;
import org.limewire.mojito2.util.HostFilter;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken.TokenProvider;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Injector;
import com.limegroup.gnutella.dht.AbstractDHTController.RandomNodeAdder;

public class AbstractDHTControllerTest extends DHTTestCase {

    private DHTControllerFactory dhtControllerFactory;

    public AbstractDHTControllerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AbstractDHTControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        DHTTestUtils.setSettings(PORT);
		Injector injector = LimeTestUtils.createInjectorNonEagerly();
		dhtControllerFactory = injector.getInstance(DHTControllerFactory.class);
    }
    
    public void testRandomNodeAdder() throws Exception {
        DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.setValue(false);
        DHTSettings.PERSIST_DHT_DATABASE.setValue(false);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        PrivilegedAccessor.setValue(DHTSettings.DHT_NODE_ADDER_DELAY, "value", 50L);
        
        DHTController controller = dhtControllerFactory.createActiveDHTNodeController(Vendor.valueOf(1),
                Version.valueOf(1), new DHTManagerStub());
        try {
            controller.start();
            MojitoDHTStub dht = new MojitoDHTStub();
            PrivilegedAccessor.setValue(controller, "dht", dht);
            
            for(int i = 0; i < 20; i++) {
                controller.addActiveDHTNode(new InetSocketAddress("localhost", 2000+i));
            }
            Thread.sleep(500);
            
            //should have started the Random node adder
            RandomNodeAdder nodeAdder = (RandomNodeAdder)PrivilegedAccessor.getValue(controller, "dhtNodeAdder");
            assertNotNull(nodeAdder);
            assertTrue("Node adder should be running", nodeAdder.isRunning());
            //should have pinged all hosts
            assertEquals(20, dht.getPingedNodesList().size());
            assertEquals(new InetSocketAddress("localhost", 2000), dht.getPingedNodesList().get(19));
            
            //try cancelling node adder and starting it again
            controller.stop();
            assertFalse("Node adder should not be running", nodeAdder.isRunning());
            controller.start();
            PrivilegedAccessor.setValue(controller, "dht", dht);
            for(int i = 0; i < 100; i++) {
                controller.addActiveDHTNode(new InetSocketAddress("localhost", 2100+i));
            }
            //see if the nodes were pinged
            Thread.sleep(500);
            assertEquals(50, dht.getPingedNodesList().size());
            assertEquals(new InetSocketAddress("localhost", 2199), dht.getPingedNodesList().get(20));
        } finally {
            controller.stop();
        }
    }
    
    private class MojitoDHTStub implements MojitoDHT {
        
        private List<SocketAddress> pingedList = new ArrayList<SocketAddress>(); 
        
        public boolean isBound() {
            return true;
        }

        @Override public void bind(InetAddress addr, int port) throws IOException {}

        @Override public void bind(int port) throws IOException {}

        @Override public void bind(SocketAddress address) throws IOException {}

//        public DHTFuture<FindValueResult> get(KUID key, DHTValueType valueType) {
//            return null;
//        }

        @Override public SocketAddress getContactAddress() {
            return null;
        }

        @Override public DHTStats getDHTStats() {
            return null;
        }

        @Override public int getExternalPort() {
            return 0;
        }

        @Override public SocketAddress getLocalAddress() {
            return null;
        }

        @Override public Contact getLocalNode() {
            return null;
        }

        @Override public KUID getLocalNodeID() {
            return null;
        }

        @Override public String getName() {
            return null;
        }

        @Override public Vendor getVendor() {
            return Vendor.UNKNOWN;
        }

        @Override public Version getVersion() {
            return Version.ZERO;
        }

        @Override public boolean isBootstrapping() {
            return false;
        }

        @Override public boolean isBootstrapped() {
            return true;
        }

        @Override public boolean isFirewalled() {
            return false;
        }

        @Override public boolean isRunning() {
            return true;
        }

        @Override public DHTFuture<PingResult> ping(SocketAddress dst) {
            pingedList.add(dst);
            return null;
        }

        @Override public DHTFuture<StoreResult> remove(KUID key) {
            return null;
        }

        @Override public void setDatabase(Database database) {}

        @Override public Database getDatabase() {
            return null;
        }
        
        @Override public void setExternalPort(int port) {}

        @Override public MessageDispatcher setMessageDispatcher(MessageDispatcherFactory messageDispatcherFactory) {
            return null;
        }

        @Override public void setMessageFactory(MessageFactory messageFactory) {}

        @Override public void setRouteTable(RouteTable routeTable) {}
        
        @Override public RouteTable getRouteTable() {
            return null;
        }
        
        @Override public BigInteger size() {
            return null;
        }

        @Override public void start() {}

        @Override public void stop() {}
        
        @Override public void close() {}
        
        @Override public void setHostFilter(HostFilter hostFilter) {
        }

        @Override public HostFilter getHostFilter() {
            return null;
        }
        
        public List<SocketAddress> getPingedNodesList(){
            return pingedList;
        }

        @Override public DHTExecutorService getDHTExecutorService() {
            return null;
        }

        @Override public void setDHTExecutorService(DHTExecutorService executors) {
        }

        @Override public DHTFuture<BootstrapResult> bootstrap(Contact node) {
            return null;
        }

        @Override public DHTFuture<BootstrapResult> bootstrap(SocketAddress dst) {
            return null;
        }
        
        @Override public DHTFuture<PingResult> findActiveContact() {
            return null;
        }

        @Override public DHTFuture<StoreResult> put(KUID key, DHTValue value) {
            return null;
        }

        @Override public KeyPair getKeyPair() {
            return null;
        }

        @Override public void setKeyPair(KeyPair keyPair) {
        }

        @Override public DHTFuture<FindValueResult> get(EntityKey entityKey) {
            return null;
        }

        @Override public DHTValueFactoryManager getDHTValueFactoryManager() {
            return null;
        }

        @Override public StorableModelManager getStorableModelManager() {
            return null;
        }

        @Override public EvictorManager getEvictorManager() {
            return null;
        }

        @Override public void setMACCalculatorRepositoryManager(MACCalculatorRepositoryManager manager) {
            
        }

        @Override public void setSecurityTokenProvider(TokenProvider tokenProvider) {
            
        }
    }
    
    
}
