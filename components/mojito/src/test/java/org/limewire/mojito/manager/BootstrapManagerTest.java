package org.limewire.mojito.manager;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestSuite;

import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.io.MessageDispatcherAdapter;
import org.limewire.mojito2.message.Message;
import org.limewire.mojito2.message.PingRequest;
import org.limewire.mojito2.settings.NetworkSettings;
import org.limewire.mojito2.util.ExceptionUtils;
import org.limewire.mojito2.util.IoUtils;

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
        
        //setup bootstrap node
        /*BOOTSTRAP_DHT = MojitoFactory.createDHT("bootstrapNode");
        bmf = new TestMessageDispatcherFactory((Context)BOOTSTRAP_DHT);
        BOOTSTRAP_DHT.setMessageDispatcher(bmf);
        BOOTSTRAP_DHT.bind(BOOTSTRAP_DHT_PORT);
        BOOTSTRAP_DHT.start();*/
        
        BOOTSTRAP_DHT = MojitoFactory.createDHT(
                "bootstrapNode", BOOTSTRAP_DHT_PORT);
        
        //setup test node
        TEST_DHT = MojitoFactory.createDHT("dht-test");
        /*tmf = new TestMessageDispatcherFactory((Context)TEST_DHT);
        TEST_DHT.setMessageDispatcher(tmf);
        TEST_DHT.bind(2000);
        TEST_DHT.start();*/
        
        TEST_DHT = MojitoFactory.createDHT("dht-test", 2000);
    }

    @Override
    protected void tearDown() {
        IoUtils.closeAll(BOOTSTRAP_DHT, TEST_DHT);
    }
    
    public void testBootstrapFailure() throws Exception {
        BOOTSTRAP_DHT.setExternalAddress(new InetSocketAddress(
                "localhost", BOOTSTRAP_DHT_PORT));
        
        // try failure first
        BOOTSTRAP_DHT.close();
        
        /*try {
            ((Context)TEST_DHT).ping(BOOTSTRAP_DHT.getLocalNode()).get();
            fail("BOOTSTRAP_DHT should not respond");
        } catch (ExecutionException err) {
            assertTrue(err.getCause() instanceof RequestTimeoutException);
        }
        
        // made sure we tried to ping them
        assertGreaterThan(1, tmf.tm.sent.size());
        Tag sent = tmf.tm.sent.get(0);
        assertEquals(sent.getNodeID(), BOOTSTRAP_DHT.getLocalNodeID());
        assertInstanceof(PingRequest.class, sent.getMessage());
        
        BootstrapResult result = TEST_DHT.bootstrap(BOOTSTRAP_DHT.getLocalNode()).get();
        assertEquals(BootstrapResult.ResultType.BOOTSTRAP_FAILED, result.getResultType());
        assertEquals(BOOTSTRAP_DHT.getLocalNodeID(), result.getContact().getNodeID());
        
        BOOTSTRAP_DHT.close();*/
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        TEST_DHT.getMessageDispatcher().addMessageDispatcherListener(
                new MessageDispatcherAdapter() {
            @Override
            public void messageSent(KUID contactId, 
                    SocketAddress dst, Message message) {
                if (message instanceof PingRequest) {
                    latch.countDown();
                }
            }
        });
        
        try {
            DHTFuture<PingEntity> future = TEST_DHT.ping(
                    BOOTSTRAP_DHT.getLocalNode(), 
                    250, TimeUnit.MILLISECONDS);
            future.get();
        } catch (ExecutionException expected) {
            assertTrue(ExceptionUtils.isCausedBy(
                    expected, TimeoutException.class));
        }
        
        if (!latch.await(250, TimeUnit.MILLISECONDS)) {
            fail("PING was not sent!");
        }
    }

    /*public void testBootstrapFromList() throws Exception{
        //try pings to a bootstrap list
        //add some bad hosts
        Set<SocketAddress> bootstrapSet = new LinkedHashSet<SocketAddress>();
        bootstrapSet.clear();
        for(int i= 1; i < 5; i++) {
            bootstrapSet.add(new InetSocketAddress("localhost", BOOTSTRAP_DHT_PORT+i));
        }
        //add good host
        bootstrapSet.add(BOOTSTRAP_DHT.getContactAddress());
        
        PingResult pong = ((Context)TEST_DHT).ping(bootstrapSet).get();
        BootstrapResult result = TEST_DHT.bootstrap(pong.getContact()).get();
        assertEquals(result.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_SUCCEEDED);
        assertEquals(BOOTSTRAP_DHT.getLocalNodeID(), result.getContact().getNodeID());
    }*/
    
    /*public void testBootstrapFromRouteTable() throws Exception{
        //try ping from RT
        RouteTable rt = TEST_DHT.getRouteTable();
        Contact node;
        for(int i= 1; i < 5; i++) {
            node = ContactFactory.createLiveContact(null, Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i), 
                    0, Contact.DEFAULT_FLAG);
            rt.add(node);
        }
        //add good node
        node = ContactFactory.createLiveContact(null, Vendor.UNKNOWN, Version.ZERO,
                BOOTSTRAP_DHT.getLocalNodeID(),
                BOOTSTRAP_DHT.getContactAddress(), 0, Contact.DEFAULT_FLAG);
        rt.add(node);
        
        // Start pinging Nodes from the RouteTable
        PingResult pong = TEST_DHT.findActiveContact().get();
        // And bootstrap from the first Node that responds
        BootstrapResult evt = TEST_DHT.bootstrap(pong.getContact()).get();
        assertEquals(evt.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_SUCCEEDED);
    }*/
    
    /*public void testBootstrapPoorRatio() throws Exception{
        //fill RT with bad nodes
        RouteTable rt = TEST_DHT.getRouteTable();
        Contact node;
        for(int i = 0; i < 100; i++) {
            node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i));
            rt.add(node);
        }
        node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                KUID.createRandomID(),
                new InetSocketAddress("localhost", 7777));
        rt.add(node);
        
        assertEquals(102, rt.size());
        PingResult pong = TEST_DHT.ping(BOOTSTRAP_DHT.getContactAddress()).get();
        BootstrapResult result = TEST_DHT.bootstrap(pong.getContact()).get();
        
        // See if RT was purged
        assertNotContains(rt.getActiveContacts(), node);
        assertEquals(result.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_SUCCEEDED);
    }*/
    
    /*public void testBootstrapPoorRatioFails() throws Exception{
        BootstrapSettings.IS_BOOTSTRAPPED_RATIO.setValue(0.7f);
        //fill RT with bad nodes
        RouteTable rt = TEST_DHT.getRouteTable();
        Contact node;
        for(int i = 0; i < 100; i++) {
            node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i));
            rt.add(node);
        }
        node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                KUID.createRandomID(),
                new InetSocketAddress("localhost", 7777));
        rt.add(node);
        
        // we lose the find node requests
        bmf.tm.filter = new FindNodeFilter(1.0);
        assertEquals(102, rt.size());
        PingResult pong = TEST_DHT.ping(BOOTSTRAP_DHT.getContactAddress()).get();
        BootstrapResult result = TEST_DHT.bootstrap(pong.getContact()).get();
        
        assertEquals(result.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_FAILED);
    }
    */
    
    /*public void testBootstrapPoorRatioSucceeds() throws Exception{
        BootstrapSettings.IS_BOOTSTRAPPED_RATIO.setValue(0.1f);
        //fill RT with bad nodes
        RouteTable rt = TEST_DHT.getRouteTable();
        Contact node;
        for(int i = 0; i < 100; i++) {
            node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i));
            rt.add(node);
        }
        node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                KUID.createRandomID(),
                new InetSocketAddress("localhost", 7777));
        rt.add(node);
        
        // we lose the find node requests
        bmf.tm.filter = new FindNodeFilter(1.0);
        assertEquals(102, rt.size());
        PingResult pong = TEST_DHT.ping(BOOTSTRAP_DHT.getContactAddress()).get();
        BootstrapResult result = TEST_DHT.bootstrap(pong.getContact()).get();
        
        assertEquals(result.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_SUCCEEDED);
    }*/
    
    /*private static class TestMessageDispatcherFactory implements MessageDispatcherFactory {
        final TestMessageDispatcher tm;
        public TestMessageDispatcherFactory(Context context) {
            tm = new TestMessageDispatcher(context);
        }
        public MessageDispatcher create(Context context) {
            return tm;
        }
        
    }*/
    
    /*private static class TestMessageDispatcher extends MessageDispatcherImpl {

        List<Tag> sent = new ArrayList<Tag>();
        List<Tag> notSent = new ArrayList<Tag>();
        List<DHTMessage> received = new ArrayList<DHTMessage>();
//        List<DHTMessage> notReceived = new ArrayList<DHTMessage>();
        volatile TestFilter filter;
        
        public TestMessageDispatcher(Context context) {
            super(context);
        }
        @Override
        protected boolean send(Tag tag) throws IOException {
            TestFilter f = filter;
            if (f != null && !f.allow(tag.getMessage())) {
                notSent.add(tag);
                return false;
            }
            sent.add(tag);
            return super.send(tag);
        }
        
        @Override
        protected void handleMessage(DHTMessage m) {
            received.add(m);
            super.handleMessage(m);
        }
    }*/
    
    /*private static interface TestFilter {
        boolean allow(DHTMessage m);
    }*/
    
    /*private static class FindNodeFilter implements TestFilter {
        private final double prob;
        FindNodeFilter(double prob) {
            this.prob = prob;
        }
        public boolean allow(DHTMessage m) {
            return !(m instanceof FindNodeRequest && Math.random() < prob);
        }
    }*/
}
