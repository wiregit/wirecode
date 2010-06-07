package org.limewire.mojito.manager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestSuite;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.MojitoUtils;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.io.MessageDispatcherAdapter;
import org.limewire.mojito.message.Message;
import org.limewire.mojito.message.NodeRequest;
import org.limewire.mojito.message.PingRequest;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.HostFilter;
import org.limewire.mojito.util.IoUtils;
import org.limewire.util.ExceptionUtils;

public class BootstrapManagerTest extends MojitoTestCase {
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/
    
    protected static final int BOOTSTRAP_DHT_PORT = 3000;

    protected static MojitoDHT BOOTSTRAP_DHT;
    
    protected static MojitoDHT TEST_DHT;
    
    //private TestMessageDispatcherFactory tmf, bmf;

    public BootstrapManagerTest(String name){
        super(name);
    }
    
    public static TestSuite suite() {
        return buildTestSuite(BootstrapManagerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void setSettings() {
        NetworkSettings.DEFAULT_TIMEOUT.setValue(200);
        NetworkSettings.MIN_TIMEOUT_RTT.setValue(200);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setSettings();
        setLocalIsPrivate(false);
        
        BOOTSTRAP_DHT = MojitoFactory.createDHT(
                "bootstrapNode", BOOTSTRAP_DHT_PORT);
        TEST_DHT = MojitoFactory.createDHT("dht-test", 2000);
        
        assertFalse(BOOTSTRAP_DHT.isBooting());
        assertFalse(BOOTSTRAP_DHT.isReady());
        
        assertFalse(TEST_DHT.isBooting());
        assertFalse(TEST_DHT.isReady());
    }

    @Override
    protected void tearDown() {
        IoUtils.closeAll(BOOTSTRAP_DHT, TEST_DHT);
    }
    
    public void testBootstrapFailure() 
            throws IOException, InterruptedException {
        
        BOOTSTRAP_DHT.setExternalAddress(new InetSocketAddress(
                "localhost", BOOTSTRAP_DHT_PORT));
        
        // Close the bootstrap Node
        BOOTSTRAP_DHT.close();
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        TEST_DHT.getMessageDispatcher().addMessageDispatcherListener(
                new MessageDispatcherAdapter() {
            @Override
            public void messageSent(KUID contactId, 
                    SocketAddress dst, Message message) {
                if (message instanceof PingRequest
                        || message instanceof NodeRequest) {
                    latch.countDown();
                }
            }
        });
        
        try {
            DHTFuture<BootstrapEntity> future = TEST_DHT.bootstrap(
                    BOOTSTRAP_DHT.getLocalhost(), 
                    250, TimeUnit.MILLISECONDS);
            future.get();
        } catch (ExecutionException expected) {
            assertTrue(ExceptionUtils.isCausedBy(
                    expected, TimeoutException.class));
        }
        
        if (!latch.await(250, TimeUnit.MILLISECONDS)) {
            fail("PING/FIND_NODE was not sent!");
        }
    }

    public void testBootstrapFromContact() 
            throws InterruptedException, ExecutionException {
        
        DHTFuture<BootstrapEntity> future = TEST_DHT.bootstrap(
                BOOTSTRAP_DHT.getLocalhost(), 
                250, TimeUnit.MILLISECONDS);
        future.get();
        
        assertFalse(TEST_DHT.isBooting());
        assertTrue(TEST_DHT.isReady());
    }
    
    public void testBootstrapFromAddress() 
            throws InterruptedException, ExecutionException {
        
        DHTFuture<BootstrapEntity> future = TEST_DHT.bootstrap(
                BOOTSTRAP_DHT.getContactAddress(), 
                250, TimeUnit.MILLISECONDS);
        future.get();
        
        assertFalse(TEST_DHT.isBooting());
        assertTrue(TEST_DHT.isReady());
    }
    
    public void testBootstrapFromRouteTable() 
            throws InterruptedException, ExecutionException {
        
        RouteTable rt = TEST_DHT.getRouteTable();
        
        // Add some BAD nodes!
        for(int i = 0; i < 5; i++) {
            Contact contact = ContactFactory.createLiveContact(null, 
                    Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i), 
                    0, Contact.DEFAULT_FLAG);
            rt.add(contact);
        }
        
        // Add one GOOD node!
        Contact contact = ContactFactory.createLiveContact(null, 
                Vendor.UNKNOWN, Version.ZERO,
                BOOTSTRAP_DHT.getContactId(),
                BOOTSTRAP_DHT.getContactAddress(), 
                0, Contact.DEFAULT_FLAG);
        rt.add(contact);
        
        DHTFuture<PingEntity> ping = TEST_DHT.findActiveContact();
        PingEntity entity = ping.get();
        
        assertFalse(TEST_DHT.isBooting());
        assertFalse(TEST_DHT.isReady());
        
        DHTFuture<BootstrapEntity> future = TEST_DHT.bootstrap(
                entity.getContact(), 
                250, TimeUnit.MILLISECONDS);
        future.get();
        
        assertFalse(TEST_DHT.isBooting());
        assertTrue(TEST_DHT.isReady());
    }
    
    public void testNoResponse100() 
            throws IOException, InterruptedException, ExecutionException {
        doTestNoResponse(5, 1);
    }
    
    public void testNoResponse75() 
            throws IOException, InterruptedException, ExecutionException {
        doTestNoResponse(5, 0.75);
    }
    
    public void testNoResponse50() 
            throws IOException, InterruptedException, ExecutionException {
        doTestNoResponse(5, 0.5);
    }
    
    public void testNoResponse25() 
            throws IOException, InterruptedException, ExecutionException {
        doTestNoResponse(5, 0.25);
    }
    
    public void testNoResponse20() 
            throws IOException, InterruptedException, ExecutionException {
        doTestNoResponse(5, 0.20);
    }
    
    public void testNoResponse15() 
            throws IOException, InterruptedException, ExecutionException {
        doTestNoResponse(5, 0.15);
    }
    
    public void testNoResponse10() 
            throws IOException, InterruptedException, ExecutionException {
        doTestNoResponse(5, 0.10);
    }
    
    public void testNoResponse5() 
            throws IOException, InterruptedException, ExecutionException {
        doTestNoResponse(5, 0.05);
    }
    
    private void doTestNoResponse(int factor, final double value) 
            throws IOException, InterruptedException, ExecutionException {
        
        HostFilter filter = new HostFilter() {
            @Override
            public boolean allow(SocketAddress addr) {
                return value >= Math.random();
            }
        };
        
        List<MojitoDHT> dhts = null;
        try {
            dhts = MojitoUtils.createBootStrappedDHTs(factor, 5000);
            
            assertFalse(BOOTSTRAP_DHT.isBooting());
            assertFalse(BOOTSTRAP_DHT.isReady());
            
            BOOTSTRAP_DHT.bootstrap(dhts.get(0).getLocalhost()).get();
            
            assertFalse(BOOTSTRAP_DHT.isBooting());
            assertTrue(BOOTSTRAP_DHT.isReady());
            
            for (MojitoDHT dht : dhts) {
                dht.setHostFilter(filter);
            }
            
            assertFalse(TEST_DHT.isBooting());
            assertFalse(TEST_DHT.isReady());
            
            BootstrapEntity entity = TEST_DHT.bootstrap(
                    BOOTSTRAP_DHT.getLocalhost()).get();
            
            assertFalse(TEST_DHT.isBooting());
            assertTrue(TEST_DHT.isReady());
            
        } finally {
            IoUtils.closeAll(dhts);
        }
    }
}
