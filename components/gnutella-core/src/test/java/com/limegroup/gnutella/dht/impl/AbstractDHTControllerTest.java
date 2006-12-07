package com.limegroup.gnutella.dht.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.dht.DHTTestCase;
import com.limegroup.gnutella.dht.impl.AbstractDHTController.RandomNodeAdder;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.concurrent.DHTExecutorService;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueEntity;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.result.BootstrapResult;
import com.limegroup.mojito.result.FindValueResult;
import com.limegroup.mojito.result.PingResult;
import com.limegroup.mojito.result.StoreResult;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.statistics.DHTStats;
import com.limegroup.mojito.util.HostFilter;

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
        AbstractDHTController controller = new ActiveDHTNodeController(1, 1);
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
        
    }
    
    private class MojitoDHTStub implements MojitoDHT {
        
        private List<SocketAddress> pingedList = new ArrayList<SocketAddress>(); 
        
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

        public int getVendor() {
            return 0;
        }

        public int getVersion() {
            return 0;
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

        public void setExternalPort(int port) {}

        public MessageDispatcher setMessageDispatcher(Class<? extends MessageDispatcher> messageDispatcher) {
            return null;
        }

        public void setMessageFactory(MessageFactory messageFactory) {}

        public void setRouteTable(RouteTable routeTable) {}
        
        public BigInteger size() {
            return null;
        }

        public void start() {}

        public void stop() {}

        public void store(OutputStream out) throws IOException {}
        
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

        public DHTFuture<PingResult> findActiveContact() {
            return null;
        }

        public Collection<DHTValueEntity> getValues() {
            return null;
        }

        public DHTFuture<StoreResult> put(KUID key, DHTValue value) {
            return null;
        }
    }
}
