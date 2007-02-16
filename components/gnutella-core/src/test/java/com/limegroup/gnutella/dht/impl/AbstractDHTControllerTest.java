package com.limegroup.gnutella.dht.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTExecutorService;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueEntityPublisher;
import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.statistics.DHTStats;
import org.limewire.mojito.util.HostFilter;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.dht.DHTManagerStub;
import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.dht.impl.AbstractDHTController.RandomNodeAdder;
import com.limegroup.gnutella.settings.DHTSettings;

public class AbstractDHTControllerTest extends DHTTestCase {

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
        setSettings();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testRandomNodeAdder() throws Exception {
        DHTSettings.PERSIST_DHT.setValue(false);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        
        AbstractDHTController controller = new ActiveDHTNodeController(
                Vendor.valueOf(1), Version.valueOf(1), new DHTManagerStub());
        try {
            controller.start();
            MojitoDHTStub dht = new MojitoDHTStub();
            PrivilegedAccessor.setValue(controller, "dht", dht);
            
            DHTSettings.DHT_NODE_ADDER_DELAY.setValue(50);
            for(int i = 0; i < 20; i++) {
                controller.addActiveDHTNode(new InetSocketAddress("localhost", 2000+i));
            }
            Thread.sleep(500);
            //should have started the Random node adder
            RandomNodeAdder nodeAdder = controller.getRandomNodeAdder();
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

        public void bind(InetAddress addr, int port) throws IOException {}

        public void bind(int port) throws IOException {}

        public void bind(SocketAddress address) throws IOException {}

        public DHTFuture<FindValueResult> get(KUID key) {
            return null;
        }

        public SocketAddress getContactAddress() {
            return null;
        }

        public DHTStats getDHTStats() {
            return null;
        }

        public int getExternalPort() {
            return 0;
        }

        public SocketAddress getLocalAddress() {
            return null;
        }

        public Contact getLocalNode() {
            return null;
        }

        public KUID getLocalNodeID() {
            return null;
        }

        public String getName() {
            return null;
        }

        public Vendor getVendor() {
            return Vendor.UNKNOWN;
        }

        public Version getVersion() {
            return Version.UNKNOWN;
        }

        public boolean isBootstrapping() {
            return false;
        }

        public boolean isBootstrapped() {
            return true;
        }

        public boolean isFirewalled() {
            return false;
        }

        public boolean isRunning() {
            return true;
        }

        public Set<KUID> keySet() {
            return null;
        }

        public DHTFuture<PingResult> ping(SocketAddress dst) {
            pingedList.add(dst);
            return null;
        }

        public DHTFuture<StoreResult> remove(KUID key) {
            return null;
        }

        public void setDatabase(Database database) {}

        public Database getDatabase() {
            return null;
        }
        
        public void setExternalPort(int port) {}

        public MessageDispatcher setMessageDispatcher(Class<? extends MessageDispatcher> messageDispatcher) {
            return null;
        }

        public void setMessageFactory(MessageFactory messageFactory) {}

        public void setRouteTable(RouteTable routeTable) {}
        
        public RouteTable getRouteTable() {
            return null;
        }
        
        public BigInteger size() {
            return null;
        }

        public void start() {}

        public void stop() {}
        
        public void close() {}
        
        public void setHostFilter(HostFilter hostFilter) {
        }

        public List<SocketAddress> getPingedNodesList(){
            return pingedList;
        }

        public DHTExecutorService getDHTExecutorService() {
            return null;
        }

        public void setDHTExecutorService(DHTExecutorService executors) {
        }

        public DHTFuture<BootstrapResult> bootstrap(Contact node) {
            return null;
        }

        public DHTFuture<BootstrapResult> bootstrap(SocketAddress dst) {
            return null;
        }
        
        public DHTFuture<PingResult> findActiveContact() {
            return null;
        }

        public Collection<DHTValueEntity> getValues() {
            return null;
        }

        public DHTFuture<StoreResult> put(KUID key, DHTValue value) {
            return null;
        }

        public DHTValueFactory getDHTValueFactory() {
            return null;
        }

        public void setDHTValueFactory(DHTValueFactory valueFactory) {
        }

        public DHTValueEntityPublisher getDHTValueEntityPublisher() {
            return null;
        }

        public void setDHTValueEntityPublisher(DHTValueEntityPublisher x) {
        }
    }
}
